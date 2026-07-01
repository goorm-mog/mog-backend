package com.mog.project.domain.summary;

import com.mog.project.domain.summary.dto.response.CardImageResponse;
import com.mog.project.domain.summary.dto.response.SummaryCardResponse;
import com.mog.project.domain.summary.service.SummaryCardService;
import com.mog.project.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "요약 카드", description = "요약 카드 데이터 조회 / 이미지 저장 API")
@RestController
@RequestMapping("/api/v1/rooms/{roomId}/summary")
@RequiredArgsConstructor
public class SummaryCardController {

    private final SummaryCardService summaryCardService;

    // GET /api/v1/rooms/{roomId}/summary
    // 카드 UI 렌더링에 필요한 전체 데이터 반환
    @Operation(
            summary = "요약 카드 데이터 조회",
            description = "요약 카드를 구성하는 전체 데이터를 반환합니다.\n\n" +
                    "- 정산이 확정된 방에서만 조회할 수 있습니다 → 미확정 시 `SETTLEMENT_NOT_CONFIRMED` 에러\n" +
                    "- `confirmedDate`: 일정 조율로 확정된 날짜 (미확정이면 null)\n" +
                    "- `confirmedPlace`: 확정된 만날 장소 (추후 구현, 현재 null)\n" +
                    "- `cardImageUrl`: 저장된 카드 이미지 URL (저장 전이면 null)",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping
    public ResponseEntity<ApiResponse<SummaryCardResponse>> getSummaryData(
            @Parameter(description = "방 ID", example = "1")
            @PathVariable Long roomId,
            @AuthenticationPrincipal String kakaoId
    ) {
        return ResponseEntity.ok(
                ApiResponse.success("요약 카드 데이터를 조회했습니다.", summaryCardService.getSummaryData(roomId, kakaoId))
        );
    }

    // POST /api/v1/rooms/{roomId}/summary/card
    // FE에서 html2canvas로 변환한 카드 이미지를 S3에 저장, 이미 있으면 교체
    @Operation(
            summary = "요약 카드 이미지 저장",
            description = "FE에서 html2canvas로 생성한 카드 이미지를 업로드합니다.\n\n" +
                    "- 지원 형식: png만 허용 (html2canvas 출력 형식)\n" +
                    "- 최대 크기: 10MB\n" +
                    "- 이미 저장된 이미지가 있으면 기존 이미지를 삭제 후 교체합니다\n" +
                    "- 저장된 URL은 카카오톡 공유 시 활용됩니다",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping(value = "/card", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CardImageResponse>> saveCardImage(
            @Parameter(description = "방 ID", example = "1")
            @PathVariable Long roomId,
            @AuthenticationPrincipal String kakaoId,
            @Parameter(description = "카드 이미지 파일 (png / 최대 10MB)")
            @RequestPart("image")MultipartFile image
            ) {
        return ResponseEntity.ok(
                ApiResponse.success("요약 카드 이미지가 저장되었습니다.",
                        summaryCardService.saveCardImage(roomId, kakaoId, image))
        );
    }


}
