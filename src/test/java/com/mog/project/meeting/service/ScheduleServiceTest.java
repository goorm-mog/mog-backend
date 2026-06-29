package com.mog.project.meeting.service;

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
import com.mog.project.domain.schedule.service.ScheduleService;
import com.mog.project.domain.schedule.service.ScheduleWebSocketPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
 
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
 
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
 
@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {
 
    @Mock
    private ScheduleSlotRepository scheduleSlotRepository;
 
    @Mock
    private ScheduleVoteRepository scheduleVoteRepository;
 
    @Mock
    private ConfirmedScheduleRepository confirmedScheduleRepository;
 
    @Mock
    private ScheduleWebSocketPublisher scheduleWebSocketPublisher;
 
    @InjectMocks
    private ScheduleService scheduleService;
 
    // ──────────────────────────────────────────
    // 1. 슬롯 등록
    // ──────────────────────────────────────────
    @Test
    @DisplayName("슬롯 등록 성공 - 기존 슬롯 삭제 후 새 슬롯 저장")
    void createSlots_success() {
        // given
        Long roomId = 1L;
        Long userId = 10L;
 
        SlotCreateRequest request = new SlotCreateRequest(List.of(
                new SlotCreateRequest.SlotItem(LocalDate.of(2025, 7, 1), LocalTime.of(18, 0)),
                new SlotCreateRequest.SlotItem(LocalDate.of(2025, 7, 2), LocalTime.of(19, 0))
        ));
 
        ScheduleSlot slot1 = ScheduleSlot.builder()
                .roomId(roomId)
                .slotDate(LocalDate.of(2025, 7, 1))
                .slotTime(LocalTime.of(18, 0))
                .build();
 
        ScheduleSlot slot2 = ScheduleSlot.builder()
                .roomId(roomId)
                .slotDate(LocalDate.of(2025, 7, 2))
                .slotTime(LocalTime.of(19, 0))
                .build();
 
        given(scheduleSlotRepository.saveAll(any())).willReturn(List.of(slot1, slot2));
 
        // when
        SlotListResponse response = scheduleService.createSlots(roomId, userId, request);
 
        // then
        assertThat(response.slots()).hasSize(2);
        verify(scheduleSlotRepository, times(1)).deleteAllByRoomId(roomId);
        verify(scheduleSlotRepository, times(1)).saveAll(any());
    }
 
    // ──────────────────────────────────────────
    // 2. 슬롯 목록 조회
    // ──────────────────────────────────────────
    @Test
    @DisplayName("슬롯 목록 조회 성공 - 투표 현황 포함")
    void getSlots_success() {
        // given
        Long roomId = 1L;
 
        ScheduleSlot slot = ScheduleSlot.builder()
                .roomId(roomId)
                .slotDate(LocalDate.of(2025, 7, 1))
                .slotTime(LocalTime.of(18, 0))
                .build();
 
        ScheduleVote vote = ScheduleVote.builder()
                .slotId(1L)
                .roomId(roomId)
                .userId(10L)
                .build();
 
        given(scheduleSlotRepository.findAllByRoomId(roomId)).willReturn(List.of(slot));
        given(scheduleVoteRepository.findAllByRoomId(roomId)).willReturn(List.of(vote));
 
        // when
        SlotListResponse response = scheduleService.getSlots(roomId);
 
        // then
        assertThat(response.slots()).hasSize(1);
        verify(scheduleSlotRepository, times(1)).findAllByRoomId(roomId);
        verify(scheduleVoteRepository, times(1)).findAllByRoomId(roomId);
    }
 
    // ──────────────────────────────────────────
    // 3. 투표 등록
    // ──────────────────────────────────────────
    @Test
    @DisplayName("투표 등록 성공 - 투표하지 않은 슬롯에 투표")
    void vote_success_register() {
        // given
        Long roomId = 1L;
        Long userId = 10L;
        Long slotId = 5L;
 
        VoteRequest request = new VoteRequest(List.of(slotId));
 
        given(scheduleVoteRepository.existsBySlotIdAndUserId(slotId, userId)).willReturn(false);
        given(scheduleVoteRepository.findAllByRoomIdAndUserId(roomId, userId)).willReturn(List.of(
                ScheduleVote.builder().slotId(slotId).roomId(roomId).userId(userId).build()
        ));
        given(scheduleSlotRepository.findAllByRoomId(roomId)).willReturn(List.of());
        given(scheduleVoteRepository.findAllByRoomId(roomId)).willReturn(List.of());
 
        // when
        VoteResponse response = scheduleService.vote(roomId, userId, request);
 
        // then
        assertThat(response.votedSlotIds()).contains(slotId);
        verify(scheduleVoteRepository, times(1)).save(any());
        verify(scheduleVoteRepository, never()).deleteBySlotIdAndUserId(any(), any());
    }
 
    @Test
    @DisplayName("투표 취소 성공 - 이미 투표한 슬롯 취소(토글)")
    void vote_success_cancel() {
        // given
        Long roomId = 1L;
        Long userId = 10L;
        Long slotId = 5L;
 
        VoteRequest request = new VoteRequest(List.of(slotId));
 
        given(scheduleVoteRepository.existsBySlotIdAndUserId(slotId, userId)).willReturn(true);
        given(scheduleVoteRepository.findAllByRoomIdAndUserId(roomId, userId)).willReturn(List.of());
        given(scheduleSlotRepository.findAllByRoomId(roomId)).willReturn(List.of());
        given(scheduleVoteRepository.findAllByRoomId(roomId)).willReturn(List.of());
 
        // when
        VoteResponse response = scheduleService.vote(roomId, userId, request);
 
        // then
        assertThat(response.votedSlotIds()).isEmpty();
        verify(scheduleVoteRepository, times(1)).deleteBySlotIdAndUserId(slotId, userId);
        verify(scheduleVoteRepository, never()).save(any());
    }
 
    // ──────────────────────────────────────────
    // 4. 일정 확정
    // ──────────────────────────────────────────
    @Test
    @DisplayName("일정 확정 성공 - 처음 확정하는 경우 새로 저장")
    void confirm_success_new() {
        // given
        Long roomId = 1L;
        Long userId = 10L;
 
        ScheduleConfirmRequest request = new ScheduleConfirmRequest(
                LocalDate.of(2025, 7, 1),
                LocalTime.of(18, 0)
        );
 
        ConfirmedSchedule confirmedSchedule = ConfirmedSchedule.builder()
                .roomId(roomId)
                .confirmedDate(request.date())
                .confirmedTime(request.time())
                .confirmedBy(userId)
                .build();
 
        given(confirmedScheduleRepository.findByRoomId(roomId)).willReturn(Optional.empty());
        given(confirmedScheduleRepository.save(any())).willReturn(confirmedSchedule);
 
        // when
        ConfirmedScheduleResponse response = scheduleService.confirm(roomId, userId, request);
 
        // then
        assertThat(response.date()).isEqualTo(LocalDate.of(2025, 7, 1));
        assertThat(response.time()).isEqualTo(LocalTime.of(18, 0));
        verify(confirmedScheduleRepository, times(1)).save(any());
    }
 
    @Test
    @DisplayName("일정 확정 성공 - 이미 확정된 경우 덮어쓰기")
    void confirm_success_update() {
        // given
        Long roomId = 1L;
        Long userId = 10L;
 
        ScheduleConfirmRequest request = new ScheduleConfirmRequest(
                LocalDate.of(2025, 7, 3),
                LocalTime.of(20, 0)
        );
 
        ConfirmedSchedule existing = ConfirmedSchedule.builder()
                .roomId(roomId)
                .confirmedDate(LocalDate.of(2025, 7, 1))
                .confirmedTime(LocalTime.of(18, 0))
                .confirmedBy(userId)
                .build();
 
        given(confirmedScheduleRepository.findByRoomId(roomId)).willReturn(Optional.of(existing));
 
        // when
        ConfirmedScheduleResponse response = scheduleService.confirm(roomId, userId, request);
 
        // then
        assertThat(response.date()).isEqualTo(LocalDate.of(2025, 7, 3));
        assertThat(response.time()).isEqualTo(LocalTime.of(20, 0));
        verify(confirmedScheduleRepository, never()).save(any());
    }
 
    // ──────────────────────────────────────────
    // 5. 확정 일정 조회
    // ──────────────────────────────────────────
    @Test
    @DisplayName("확정 일정 조회 성공")
    void getConfirmedSchedule_success() {
        // given
        Long roomId = 1L;
 
        ConfirmedSchedule confirmed = ConfirmedSchedule.builder()
                .roomId(roomId)
                .confirmedDate(LocalDate.of(2025, 7, 1))
                .confirmedTime(LocalTime.of(18, 0))
                .confirmedBy(10L)
                .build();
 
        given(confirmedScheduleRepository.findByRoomId(roomId)).willReturn(Optional.of(confirmed));
 
        // when
        ConfirmedScheduleResponse response = scheduleService.getConfirmedSchedule(roomId);
 
        // then
        assertThat(response.date()).isEqualTo(LocalDate.of(2025, 7, 1));
        assertThat(response.time()).isEqualTo(LocalTime.of(18, 0));
    }
 
    @Test
    @DisplayName("확정 일정 조회 실패 - 아직 확정된 일정 없음")
    void getConfirmedSchedule_fail_notFound() {
        // given
        Long roomId = 1L;
 
        given(confirmedScheduleRepository.findByRoomId(roomId)).willReturn(Optional.empty());
 
        // when & then
        assertThatThrownBy(() -> scheduleService.getConfirmedSchedule(roomId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("아직 확정된 일정이 없습니다.");
    }
}