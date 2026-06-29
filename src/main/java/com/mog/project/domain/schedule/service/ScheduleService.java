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
 
    // ──────────────────────────────────────────
    // 1. 슬롯 등록 (방장)
    // ──────────────────────────────────────────
    @Transactional
    public SlotListResponse createSlots(Long roomId, Long userId, SlotCreateRequest request) {
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
    public VoteResponse vote(Long roomId, Long userId, VoteRequest request) {
        for (Long slotId : request.slotIds()) {
            if (scheduleVoteRepository.existsBySlotIdAndUserId(slotId, userId)) {
                // 이미 투표한 슬롯 → 취소
                scheduleVoteRepository.deleteBySlotIdAndUserId(slotId, userId);
            } else {
                // 투표 안 한 슬롯 → 등록
                scheduleVoteRepository.save(
                        ScheduleVote.builder()
                                .slotId(slotId)
                                .roomId(roomId)
                                .userId(userId)
                                .build()
                );
            }
        }
 
        // 현재 유저의 투표된 슬롯 ID 목록
        List<Long> votedSlotIds = scheduleVoteRepository
                .findAllByRoomIdAndUserId(roomId, userId)
                .stream()
                .map(ScheduleVote::getSlotId)
                .toList();
 
        // 전체 투표 현황 WS 브로드캐스트
        SlotListResponse slotListResponse = getSlots(roomId);
        scheduleWebSocketPublisher.publishVoteUpdate(roomId, slotListResponse);
 
        return new VoteResponse(votedSlotIds);
    }
 
    // ──────────────────────────────────────────
    // 4. 일정 확정 (방장) - upsert + WS 브로드캐스트
    // ──────────────────────────────────────────
    @Transactional
    public ConfirmedScheduleResponse confirm(Long roomId, Long userId, ScheduleConfirmRequest request) {
        ConfirmedSchedule confirmed = confirmedScheduleRepository.findByRoomId(roomId)
                .map(existing -> {
                    // 이미 확정된 경우 → 덮어쓰기
                    existing.update(request.date(), request.time());
                    return existing;
                })
                .orElseGet(() ->
                        // 처음 확정하는 경우 → 새로 저장
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
 
        // 확정 일정 WS 브로드캐스트
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