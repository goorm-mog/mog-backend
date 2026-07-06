package com.mog.project.domain.settlement.controller;


import com.mog.project.domain.settlement.dto.response.SettlementResponse;
import com.mog.project.domain.settlement.service.SettlementService;
import com.mog.project.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "정산", description = "정산 계산 / 조회 / 확정 API")
@RestController
@RequestMapping("/api/v1/rooms/{roomId}/settlement")
@RequiredArgsConstructor
public class SettlementController {
    private final SettlementService settlementService;

    // POST /api/v1/roms/{roomId}/settlement
    // 정산 계산 및 생성
    @Operation(
            summary = "정산 계산",
            description = "방의 전체 차수 기록을 기반으로 멤버별 정산 금액을 계산합니다.\n\n" +
                    "- 방 멤버라면 누구나 호출할 수 있습니다\n" +
                    "- 이미 정산이 존재하면 삭제 후 재계산됩니다\n" +
                    "- 차수 기록이 없으면 `NO_RECORDS` 에러가 반환됩니다",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping
    public ResponseEntity<ApiResponse<SettlementResponse>> calculateSettlement(
            @Parameter(description = "방 ID", example = "1")
            @PathVariable Long roomId,
            @AuthenticationPrincipal String kakaoId
    ) {
        return ResponseEntity.status(201).body(
                ApiResponse.success("정산이 계산되었습니다.", settlementService.calculateAndCreate(roomId, kakaoId))
        );
    }

    // GET /api/v1/rooms/{roomId}/settlement
    // 정산 조회
    @Operation(
            summary = "정산 조회",
            description = "계산된 정산 결과를 조회합니다.\n\n" +
                    "- 정산이 존재하지 않으면 `SETTLEMENT_NOT_FOUND` 에러가 반환됩니다\n" +
                    "- `isConfirmed`: 방장이 확정한 정산인지 여부\n" +
                    "- `detail`: 차수별 금액 상세 내역",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping
    public ResponseEntity<ApiResponse<SettlementResponse>> getSettlement(
            @Parameter(description = "방 ID", example = "1")
            @PathVariable Long roomId,
            @AuthenticationPrincipal String kakaoId
    ) {
        return ResponseEntity.ok(
                ApiResponse.success("정산을 조회했습니다.", settlementService.getSettlement(roomId, kakaoId))
        );
    }

    // PATCH /api/v1/rooms/{roomId}/settlement/confirm
    // 정산 확정
    @Operation(
            summary = "정산 확정",
            description = "방장이 정산을 확정합니다.\n\n" +
                    "- 방장(`LEADER`)만 호출할 수 있습니다 → 아니면 `NOT_HOST` 에러\n" +
                    "- 이미 확정된 정산은 다시 확정할 수 없습니다 → `ALREADY_CONFIRMED` 에러\n" +
                    "- 정산이 없으면 `SETTLEMENT_NOT_FOUND` 에러가 반환됩니다",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PatchMapping("/confirm")
    public ResponseEntity<ApiResponse<SettlementResponse>> confirmSettlement(
            @Parameter(description = "방 ID", example = "1")
            @PathVariable Long roomId,
            @AuthenticationPrincipal String kakaoId
    ) {
        return ResponseEntity.ok(
                ApiResponse.success("정산이 확정되었습니다.", settlementService.confirmSettlement(roomId, kakaoId))
        );
    }

}
