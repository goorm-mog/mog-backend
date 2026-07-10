package com.mog.project.domain.meeting.controller;

import com.mog.project.global.response.ApiResponse;
import com.mog.project.domain.meeting.dto.request.MeetingRecordCreateRequest;
import com.mog.project.domain.meeting.dto.request.MeetingRecordUpdateRequest;
import com.mog.project.domain.meeting.dto.response.MeetingRecordListResponse;
import com.mog.project.domain.meeting.dto.response.MeetingRecordResponse;
import com.mog.project.domain.meeting.service.MeetingRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.mog.project.domain.meeting.dto.response.OcrResponse;
import com.mog.project.domain.meeting.service.OcrService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "만남 기록", description = "만남 기록 생성 / 조회 / 수정 / 삭제 API")
@RestController
@RequestMapping("/api/v1/rooms/{roomId}/records")
@RequiredArgsConstructor
public class MeetingRecordController {

    private final MeetingRecordService meetingRecordService;
    private final OcrService ocrService;

    @Operation(
            summary = "만남 기록 목록 조회",
            description = "해당 방의 사진 목록과 전체 차수 기록을 한 번에 반환합니다.\n\n" +
                    "- `photos`: 방에 등록된 사진 목록 (최대 3장)\n" +
                    "- `records`: 차수별 기록 목록 (seq 오름차순)\n" +
                    "- `totalCost`: 해당 차수의 전체 비용 합계\n" +
                    "- `payer`: 결제자 정보 (없으면 null)\n" +
                    "- `menuItems`: 세부 메뉴 목록 (없으면 빈 배열)\n" +
                    "  - `itemName`: 메뉴 이름\n" +
                    "  - `quantity`: 수량\n" +
                    "  - `price`: 단가\n" +
                    "  - `totalPrice`: 합계 금액 (quantity × price)"
    )
    @GetMapping
    public ResponseEntity<ApiResponse<MeetingRecordListResponse>> getRecords(
            @Parameter(description = "방 ID", example = "1") @PathVariable Long roomId,
            @AuthenticationPrincipal String kakaoId
    ) {
        return ResponseEntity.ok(ApiResponse.success("만남 기록 목록을 조회했습니다.", meetingRecordService.getRecords(roomId, kakaoId)));
    }

    @Operation(
            summary = "만남 기록 생성",
            description = "새로운 차수 기록을 추가합니다.\n\n" +
                    "- `seq`(차수)는 자동으로 부여됩니다 (기존 최대 seq + 1)\n" +
                    "- `payer`는 선택값입니다 (없으면 null)\n" +
                    "- `participants`는 최소 1명 이상 필수입니다\n" +
                    "- `amount`는 0 이상의 정수여야 합니다\n" +
                    "- `menuItems`는 선택값입니다 (없으면 생략 또는 null)\n" +
                    "  - `itemName`: 메뉴 이름 (필수)\n" +
                    "  - `quantity`: 수량, 1 이상 (필수)\n" +
                    "  - `price`: 단가, 0 이상 (필수)"
    )
    @PostMapping
    public ResponseEntity<ApiResponse<MeetingRecordResponse>> createRecord(
            @Parameter(description = "방 ID", example = "1") @PathVariable Long roomId,
            @AuthenticationPrincipal String kakaoId,
            @RequestBody @Valid MeetingRecordCreateRequest request
    ) {
        return ResponseEntity.status(201).body(
                ApiResponse.success("만남 기록을 생성했습니다.", meetingRecordService.createRecord(roomId, kakaoId, request))
        );
    }

    @Operation(
            summary = "만남 기록 수정",
            description = "특정 차수 기록을 수정합니다. (PATCH 방식)\n\n" +
                    "- 수정하지 않을 필드는 요청에서 **생략하거나 null**로 보내면 기존 값이 유지됩니다\n" +
                    "- `participants`를 포함하면 기존 참여자 목록 전체가 교체됩니다\n" +
                    "- `participants`를 생략하면 기존 참여자 목록이 유지됩니다\n" +
                    "- `menuItems`를 포함하면 기존 메뉴 목록 전체가 교체됩니다\n" +
                    "- `menuItems`를 생략하면 기존 메뉴 목록이 유지됩니다"
    )
    @PatchMapping("/{recordId}")
    public ResponseEntity<ApiResponse<MeetingRecordResponse>> updateRecord(
            @Parameter(description = "방 ID", example = "1") @PathVariable Long roomId,
            @Parameter(description = "수정할 기록 ID", example = "3") @PathVariable Long recordId,
            @AuthenticationPrincipal String kakaoId,
            @RequestBody @Valid MeetingRecordUpdateRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success("만남 기록을 수정했습니다.", meetingRecordService.updateRecord(roomId, recordId, kakaoId, request))
        );
    }

    @Operation(
            summary = "만남 기록 삭제",
            description = "특정 차수 기록을 삭제합니다.\n\n" +
                    "- 삭제 후 뒤에 있는 차수들의 seq가 자동으로 1씩 감소합니다\n" +
                    "  (예: 1, 2, 3차 중 2차 삭제 → 3차가 2차로 변경)"
    )
    @DeleteMapping("/{recordId}")
    public ResponseEntity<ApiResponse<Void>> deleteRecord(
            @Parameter(description = "방 ID", example = "1") @PathVariable Long roomId,
            @Parameter(description = "삭제할 기록 ID", example = "3") @PathVariable Long recordId,
            @AuthenticationPrincipal String kakaoId
    ) {
        meetingRecordService.deleteRecord(roomId, recordId, kakaoId);
        return ResponseEntity.ok(ApiResponse.success("만남 기록을 삭제했습니다."));
    }

    // POST /api/v1/rooms/{roomId}/records/ocr
    // 영수증 이미지를 받아 Gemini로 분석 후 가게명, 금액, 메뉴를 반환
    @Operation(
            summary = "영수증 OCR",
            description = "영수증 이미지를 업로드하면 Gemini AI가 분석하여 정보를 추출합니다.\n\n" +
                    "- 지원 형식: jpg, jpeg, png, gif, webp\n" +
                    "- 최대 크기: 10MB\n" +
                    "- `storeName`: 가게명 (인식 불가 시 null)\n" +
                    "- `totalAmount`: 총 금액 (인식 불가 시 0)\n" +
                    "- `items[].count`: 수량 (영수증에 없으면 null)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping(value = "/ocr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<OcrResponse>> analyzeReceipt(
            @Parameter(description = "방 ID", example = "1")
            @PathVariable Long roomId,

            @Parameter(description = "영수증 이미지 파일 (jpa, png, gif, webp / 최대 10MB)")
            @RequestPart("image") MultipartFile image
    ) {
        return ResponseEntity.ok(ApiResponse.success("영수증 분석이 완료되었습니다.", ocrService.analyzeReceipt(image)));
    }
}
