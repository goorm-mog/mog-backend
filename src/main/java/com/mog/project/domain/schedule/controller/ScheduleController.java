package com.mog.project.domain.schedule.controller;

import com.mog.project.domain.schedule.dto.ConfirmedScheduleResponse;
import com.mog.project.domain.schedule.dto.ScheduleConfirmRequest;
import com.mog.project.domain.schedule.dto.ScheduleStatusResponse;
import com.mog.project.domain.schedule.dto.SlotCreateRequest;
import com.mog.project.domain.schedule.dto.SlotListResponse;
import com.mog.project.domain.schedule.dto.VoteRequest;
import com.mog.project.domain.schedule.dto.VoteResponse;
import com.mog.project.domain.schedule.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
 
@Tag(name = "일정 조율", description = "모임 일정 투표 및 확정 API")
@RestController
@RequestMapping("/api/rooms/{roomId}/schedule")
@RequiredArgsConstructor
public class ScheduleController {
 
    private final ScheduleService scheduleService;
 
    // ──────────────────────────────────────────
    // 1. 슬롯 등록 (방장)
    // ──────────────────────────────────────────
    @Operation(summary = "슬롯 등록", description = "방장이 투표 가능한 날짜/시간 슬롯을 등록합니다. 기존 슬롯은 전부 삭제 후 재등록됩니다.")
    @PostMapping("/slots")
    public ResponseEntity<SlotListResponse> createSlots(
            @PathVariable Long roomId,
            @AuthenticationPrincipal String kakaoId,
            @RequestBody SlotCreateRequest request) {
        return ResponseEntity.ok(scheduleService.createSlots(roomId, kakaoId, request));
    }
 
    // ──────────────────────────────────────────
    // 2. 슬롯 목록 조회 (투표 현황 포함)
    // ──────────────────────────────────────────
    @Operation(summary = "슬롯 목록 조회", description = "투표 가능한 슬롯 목록과 각 슬롯의 투표 현황을 조회합니다. slotId, voteCount, votedUserIds, totalParticipants 포함.")
    @GetMapping("/slots")
    public ResponseEntity<SlotListResponse> getSlots(
            @PathVariable Long roomId) {
        return ResponseEntity.ok(scheduleService.getSlots(roomId));
    }
 
    // ──────────────────────────────────────────
    // 3. 투표 등록 / 취소 (토글)
    // ──────────────────────────────────────────
    @Operation(summary = "투표 등록/취소", description = "슬롯에 투표하거나 취소합니다. 이미 투표한 슬롯은 취소, 투표하지 않은 슬롯은 등록됩니다.(토글)")
    @PostMapping("/votes")
    public ResponseEntity<VoteResponse> vote(
            @PathVariable Long roomId,
            @AuthenticationPrincipal String kakaoId,
            @RequestBody VoteRequest request) {
        return ResponseEntity.ok(scheduleService.vote(roomId, kakaoId, request));
    }
 
    // ──────────────────────────────────────────
    // 4. 일정 확정 (방장)
    // ──────────────────────────────────────────
    @Operation(summary = "일정 확정", description = "방장이 최종 일정을 확정합니다. 투표 여부와 무관하게 자유롭게 확정 가능하며, 이미 확정된 경우 덮어쓰기됩니다. 참여자 전원 카카오 톡캘린더에 자동 등록됩니다.")
    @PatchMapping("/confirm")
    public ResponseEntity<ConfirmedScheduleResponse> confirm(
            @PathVariable Long roomId,
            @AuthenticationPrincipal String kakaoId,
            @RequestBody ScheduleConfirmRequest request) {
        return ResponseEntity.ok(scheduleService.confirm(roomId, kakaoId, request));
    }
 
    // ──────────────────────────────────────────
    // 5. 확정 일정 조회
    // ──────────────────────────────────────────
    @Operation(summary = "확정 일정 조회", description = "방장이 확정한 일정을 조회합니다. 아직 확정되지 않은 경우 404를 반환합니다.")
    @GetMapping("/confirm")
    public ResponseEntity<ConfirmedScheduleResponse> getConfirmedSchedule(
            @PathVariable Long roomId) {
        return ResponseEntity.ok(scheduleService.getConfirmedSchedule(roomId));
    }
 
    // ──────────────────────────────────────────
    // 6. 현재 진행 단계 조회
    // ──────────────────────────────────────────
    @Operation(summary = "현재 진행 단계 조회", description = "현재 방이 어느 단계인지 조회합니다. WAITING | SCHEDULE_VOTING | DEPARTURE_INPUT | MIDPOINT_FINDING | COMPLETED")
    @GetMapping("/status")
    public ResponseEntity<ScheduleStatusResponse> getStatus(
            @PathVariable Long roomId) {
        return ResponseEntity.ok(scheduleService.getStatus(roomId));
    }
}
