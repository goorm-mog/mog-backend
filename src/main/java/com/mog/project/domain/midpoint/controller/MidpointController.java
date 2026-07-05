package com.mog.project.domain.midpoint.controller;

import com.mog.project.domain.midpoint.dto.ConfirmPlaceRequest;
import com.mog.project.domain.midpoint.dto.ConfirmedPlaceResponse;
import com.mog.project.domain.midpoint.dto.DepartureLocationListResponse;
import com.mog.project.domain.midpoint.dto.DepartureLocationRequest;
import com.mog.project.domain.midpoint.dto.DepartureLocationResponse;
import com.mog.project.domain.midpoint.dto.MiddlePointResponse;
import com.mog.project.domain.midpoint.dto.TravelTimeResponse;
import com.mog.project.domain.midpoint.service.MidpointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
 
@Tag(name = "중간지점", description = "출발지 등록 및 중간지점 계산 API")
@RestController
@RequestMapping("/api/rooms/{roomId}")
@RequiredArgsConstructor
public class MidpointController {
 
    private final MidpointService midpointService;
 
    // ──────────────────────────────────────────
    // 1. 출발지 등록
    // POST /api/rooms/{roomId}/departure
    // ──────────────────────────────────────────
    @Operation(summary = "출발지 등록", description = "참여자가 출발지와 이동수단을 등록합니다. 1인 1출발지만 가능하며 이미 등록된 경우 409를 반환합니다.")
    @PostMapping("/departure")
    public ResponseEntity<DepartureLocationResponse> registerDeparture(
            @PathVariable Long roomId,
            @AuthenticationPrincipal String kakaoId,
            @RequestBody DepartureLocationRequest request) {
        return ResponseEntity.ok(midpointService.registerDeparture(roomId, kakaoId, request));
    }
 
    // ──────────────────────────────────────────
    // 2. 출발지 수정
    // PATCH /api/rooms/{roomId}/departure
    // ──────────────────────────────────────────
    @Operation(summary = "출발지 수정", description = "참여자가 본인의 출발지를 수정합니다. 등록된 출발지가 없으면 404를 반환합니다.")
    @PatchMapping("/departure")
    public ResponseEntity<DepartureLocationResponse> updateDeparture(
            @PathVariable Long roomId,
            @AuthenticationPrincipal String kakaoId,
            @RequestBody DepartureLocationRequest request) {
        return ResponseEntity.ok(midpointService.updateDeparture(roomId, kakaoId, request));
    }
 
    // ──────────────────────────────────────────
    // 3. 출발지 목록 조회
    // GET /api/rooms/{roomId}/departure
    // ──────────────────────────────────────────
    @Operation(summary = "출발지 목록 조회", description = "방의 모든 참여자 출발지 목록을 조회합니다. 주소(address) 포함.")
    @GetMapping("/departure")
    public ResponseEntity<DepartureLocationListResponse> getDepartures(
            @PathVariable Long roomId) {
        return ResponseEntity.ok(midpointService.getDepartures(roomId));
    }
 
    // ──────────────────────────────────────────
    // 4. 중간지점 계산
    // POST /api/rooms/{roomId}/midpoint/calculate
    // ──────────────────────────────────────────
    @Operation(summary = "중간지점 계산", description = "참여자 출발지 위도/경도 평균으로 중간지점을 계산합니다. 이미 계산된 경우 덮어쓰기됩니다.")
    @PostMapping("/midpoint/calculate")
    public ResponseEntity<MiddlePointResponse> calculateMiddlePoint(
            @PathVariable Long roomId,
            @AuthenticationPrincipal String kakaoId) {
        return ResponseEntity.ok(midpointService.calculateMiddlePoint(roomId, kakaoId));
    }
 
    // ──────────────────────────────────────────
    // 5. 중간지점 조회
    // GET /api/rooms/{roomId}/midpoint
    // ──────────────────────────────────────────
    @Operation(summary = "중간지점 조회", description = "계산된 중간지점 좌표를 조회합니다. 아직 계산되지 않은 경우 404를 반환합니다.")
    @GetMapping("/midpoint")
    public ResponseEntity<MiddlePointResponse> getMiddlePoint(
            @PathVariable Long roomId) {
        return ResponseEntity.ok(midpointService.getMiddlePoint(roomId));
    }
 
    // ──────────────────────────────────────────
    // 6. 장소 확정 (방장)
    // POST /api/rooms/{roomId}/midpoint/confirm
    // ──────────────────────────────────────────
    @Operation(summary = "장소 확정", description = "방장이 만날 장소를 확정합니다. 이미 확정된 경우 덮어쓰기됩니다.")
    @PostMapping("/midpoint/confirm")
    public ResponseEntity<ConfirmedPlaceResponse> confirmPlace(
            @PathVariable Long roomId,
            @AuthenticationPrincipal String kakaoId,
            @RequestBody ConfirmPlaceRequest request) {
        return ResponseEntity.ok(midpointService.confirmPlace(roomId, kakaoId, request));
    }
 
    // ──────────────────────────────────────────
    // 7. 인원별 소요시간 계산 (방장)
    // POST /api/rooms/{roomId}/midpoint/travel-times
    // ──────────────────────────────────────────
    @Operation(summary = "인원별 소요시간 계산", description = "방장이 카카오 모빌리티 API를 통해 참여자별 중간지점까지의 소요시간을 계산합니다. 중간지점이 먼저 계산되어 있어야 합니다.")
    @PostMapping("/midpoint/travel-times")
    public ResponseEntity<TravelTimeResponse> calculateTravelTimes(
            @PathVariable Long roomId,
            @AuthenticationPrincipal String kakaoId) {
        return ResponseEntity.ok(midpointService.calculateTravelTimes(roomId, kakaoId));
    }
}