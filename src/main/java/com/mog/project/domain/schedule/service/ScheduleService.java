package com.mog.project.domain.schedule.service;

import com.mog.project.domain.midpoint.repository.DepartureLocationRepository;
import com.mog.project.domain.midpoint.repository.MiddlePointRepository;
import com.mog.project.domain.notification.entity.NotificationType;
import com.mog.project.domain.notification.service.NotificationService;
import com.mog.project.domain.room.entity.Room;
import com.mog.project.domain.room.entity.RoomMember;
import com.mog.project.domain.room.repository.RoomMemberRepository;
import com.mog.project.domain.room.repository.RoomRepository;
import com.mog.project.domain.schedule.dto.ScheduleConfirmRequest;
import com.mog.project.domain.schedule.dto.ScheduleStatusResponse;
import com.mog.project.domain.schedule.dto.SlotCreateRequest;
import com.mog.project.domain.schedule.dto.VoteRequest;
import com.mog.project.domain.schedule.dto.ConfirmedScheduleResponse;
import com.mog.project.domain.schedule.dto.SlotListResponse;
import com.mog.project.domain.schedule.dto.VoteResponse;
import com.mog.project.domain.schedule.entity.ConfirmedSchedule;
import com.mog.project.domain.schedule.entity.ScheduleSlot;
import com.mog.project.domain.schedule.entity.ScheduleVote;
import com.mog.project.domain.schedule.repository.ConfirmedScheduleRepository;
import com.mog.project.domain.schedule.repository.ScheduleSlotRepository;
import com.mog.project.domain.schedule.repository.ScheduleVoteRepository;
import com.mog.project.domain.user.entity.User;
import com.mog.project.domain.user.repository.UserRepository;
import com.mog.project.global.kakao.KakaoCalendarClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import java.util.List;
 
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {
 
    private final ScheduleSlotRepository scheduleSlotRepository;
    private final ScheduleVoteRepository scheduleVoteRepository;
    private final ConfirmedScheduleRepository confirmedScheduleRepository;
    private final ScheduleWebSocketPublisher scheduleWebSocketPublisher;
    private final UserRepository userRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final RoomRepository roomRepository;
    private final KakaoCalendarClient kakaoCalendarClient;
    private final DepartureLocationRepository departureLocationRepository;
    private final MiddlePointRepository middlePointRepository;
    private final NotificationService notificationService;
 
    // kakaoId → userId 변환 공통 메서드
    private Long getUserId(String kakaoId) {
        return userRepository.findByKakaoId(kakaoId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."))
                .getUserId();
    }
 
    // 방 참여자 수 조회 공통 메서드
    private int getTotalParticipants(Long roomId) {
        return roomMemberRepository.findByRoomRoomId(roomId).size();
    }
 
    // ──────────────────────────────────────────
    // 1. 슬롯 등록 (방장)
    // ──────────────────────────────────────────
    @Transactional
    public SlotListResponse createSlots(Long roomId, String kakaoId, SlotCreateRequest request) {
        Long userId = getUserId(kakaoId);
        scheduleSlotRepository.deleteAllByRoomId(roomId);
 
        List<ScheduleSlot> slots = request.slots().stream()
                .map(slot -> ScheduleSlot.builder()
                        .roomId(roomId)
                        .slotDate(slot.date())
                        .slotTime(slot.time())
                        .build())
                .toList();
 
        List<ScheduleSlot> savedSlots = scheduleSlotRepository.saveAll(slots);
        return SlotListResponse.from(roomId, savedSlots, List.of());
    }
 
    // ──────────────────────────────────────────
    // 2. 슬롯 목록 조회 (투표 현황 포함)
    // ──────────────────────────────────────────
    public SlotListResponse getSlots(Long roomId) {
        List<ScheduleSlot> slots = scheduleSlotRepository.findAllByRoomId(roomId);
        List<ScheduleVote> votes = scheduleVoteRepository.findAllByRoomId(roomId);
        return SlotListResponse.from(roomId, slots, votes);
    }
 
    // ──────────────────────────────────────────
    // 3. 투표 등록 / 취소 (토글) + WS 브로드캐스트
    // ──────────────────────────────────────────
    @Transactional
    public VoteResponse vote(Long roomId, String kakaoId, VoteRequest request) {
        Long userId = getUserId(kakaoId);
 
        for (Long slotId : request.slotIds()) {
            if (scheduleVoteRepository.existsBySlotIdAndUserId(slotId, userId)) {
                scheduleVoteRepository.deleteBySlotIdAndUserId(slotId, userId);
            } else {
                scheduleVoteRepository.save(
                        ScheduleVote.builder()
                                .slotId(slotId)
                                .roomId(roomId)
                                .userId(userId)
                                .build()
                );
            }
        }
 
        List<Long> votedSlotIds = scheduleVoteRepository
                .findAllByRoomIdAndUserId(roomId, userId)
                .stream()
                .map(ScheduleVote::getSlotId)
                .toList();
 
        SlotListResponse slotListResponse = getSlots(roomId);
        scheduleWebSocketPublisher.publishVoteUpdate(roomId, slotListResponse);
 
        return new VoteResponse(votedSlotIds);
    }
 
    // ──────────────────────────────────────────
    // 4. 일정 확정 (방장) - upsert + WS 브로드캐스트 + 톡캘린더 연동
    // ──────────────────────────────────────────
    @Transactional
    public ConfirmedScheduleResponse confirm(Long roomId, String kakaoId, ScheduleConfirmRequest request) {
        Long userId = getUserId(kakaoId);
 
        ConfirmedSchedule confirmed = confirmedScheduleRepository.findByRoomId(roomId)
                .map(existing -> {
                    existing.update(request.date(), request.time());
                    return existing;
                })
                .orElseGet(() ->
                        confirmedScheduleRepository.save(
                                ConfirmedSchedule.builder()
                                        .roomId(roomId)
                                        .confirmedDate(request.date())
                                        .confirmedTime(request.time())
                                        .confirmedBy(userId)
                                        .build()
                        )
                );
 
        // 카카오 톡캘린더 연동 - 참여자 전원 캘린더에 일정 등록
        List<RoomMember> roomMembers = roomMemberRepository.findByRoomRoomId(roomId);
        for (RoomMember member : roomMembers) {
            User memberUser = member.getUser();
            try {
                if (confirmed.getKakaoEventId() == null) {
                    String eventId = kakaoCalendarClient.createEvent(
                            memberUser,
                            "모임 일정",
                            request.date(),
                            request.time()
                    );
                    if (memberUser.getUserId().equals(userId)) {
                        confirmed.updateKakaoEventId(eventId);
                    }
                } else {
                    kakaoCalendarClient.updateEvent(
                            memberUser,
                            confirmed.getKakaoEventId(),
                            "모임 일정",
                            request.date(),
                            request.time()
                    );
                }
            } catch (Exception e) {
                // 톡캘린더 등록 실패해도 일정 확정은 유지
            }
        }
 
        ConfirmedScheduleResponse response = ConfirmedScheduleResponse.from(confirmed);
        scheduleWebSocketPublisher.publishConfirm(roomId, response);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("방을 찾을 수 없습니다."));
        String message = "[" + room.getRoomName() + "] 날짜가 확정됐습니다.";
        roomMembers.forEach(member -> notificationService.send(
                member.getUser().getUserId(),
                NotificationType.DATE_CONFIRMED,
                message,
                roomId
        ));

        return response;
    }
 
    // ──────────────────────────────────────────
    // 5. 확정 일정 조회
    // ──────────────────────────────────────────
    public ConfirmedScheduleResponse getConfirmedSchedule(Long roomId) {
        ConfirmedSchedule confirmed = confirmedScheduleRepository.findByRoomId(roomId)
                .orElseThrow(() -> new IllegalArgumentException("아직 확정된 일정이 없습니다."));
        return ConfirmedScheduleResponse.from(confirmed);
    }
 
    // ──────────────────────────────────────────
    // 6. 현재 진행 단계 조회
    // ──────────────────────────────────────────
    public ScheduleStatusResponse getStatus(Long roomId) {
        // 슬롯이 없으면 → 대기 중
        List<ScheduleSlot> slots = scheduleSlotRepository.findAllByRoomId(roomId);
        if (slots.isEmpty()) {
            return ScheduleStatusResponse.waiting(roomId);
        }
 
        // 일정 확정이 안 됐으면 → 날짜/시간 투표 중
        boolean isScheduleConfirmed = confirmedScheduleRepository.existsByRoomId(roomId);
        if (!isScheduleConfirmed) {
            return ScheduleStatusResponse.scheduleVoting(roomId);
        }
 
        // 출발지 미입력 인원이 있으면 → 출발지 입력 중
        int totalParticipants = getTotalParticipants(roomId);
        long submittedCount = departureLocationRepository.countByRoomId(roomId);
        if (submittedCount < totalParticipants) {
            return ScheduleStatusResponse.departureInput(roomId);
        }
 
        // 중간지점 미계산이면 → 중간지점 찾기 중
        boolean isMiddlePointCalculated = middlePointRepository.existsByRoomId(roomId);
        if (!isMiddlePointCalculated) {
            return ScheduleStatusResponse.midpointFinding(roomId);
        }
 
        // 모두 완료
        return ScheduleStatusResponse.completed(roomId);
    }
}