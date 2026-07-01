package com.mog.project.domain.schedule.service;

import com.mog.project.domain.schedule.dto.ScheduleConfirmRequest;
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
 
    // kakaoId → userId 변환 공통 메서드
    private Long getUserId(String kakaoId) {
        return userRepository.findByKakaoId(kakaoId)
                .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없습니다."))
                .getUserId();
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
    // 4. 일정 확정 (방장) - upsert + WS 브로드캐스트
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
 
        // TODO: 카카오 톡캘린더 API 연동 후 kakaoEventId 업데이트
        // String eventId = kakaoCalendarClient.createEvent(confirmed);
        // confirmed.updateKakaoEventId(eventId);
 
        ConfirmedScheduleResponse response = ConfirmedScheduleResponse.from(confirmed);
        scheduleWebSocketPublisher.publishConfirm(roomId, response);
 
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
}