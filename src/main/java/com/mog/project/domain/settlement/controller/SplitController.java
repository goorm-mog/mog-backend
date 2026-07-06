package com.mog.project.domain.settlement.controller;

import com.mog.project.domain.settlement.dto.request.SplitRequest;
import com.mog.project.domain.settlement.dto.response.SplitMemberResponse;
import com.mog.project.domain.settlement.dto.response.SplitResponse;
import com.mog.project.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Tag(name = "정산", description = "정산 계산 / 조회 / 확정 API")
@RestController
@RequestMapping("/api/v1/settlement")
@RequiredArgsConstructor
public class SplitController {

    @Operation(
            summary = "1/N 금액 분배 계산",
            description = "총 금액을 인원수로 나눠 각 멤버의 부담액을 계산합니다.\n\n" +
                    "- 나머지가 발생하면 랜덤으로 선택된 1명에게 1원이 추가됩니다.\n" +
                    "- 예) 10000원 / 3명 → 3334원, 3333원, 3333원",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/split")
    public ResponseEntity<ApiResponse<SplitResponse>> split(
            @RequestBody @Valid SplitRequest request
    ) {
        int total = request.totalAmount();
        List<String> members = new ArrayList<>(request.members());
        int count = members.size();

        int base = total / count;
        int remainder = total % count;

        // 랜덤으로 순서를 섞어 첫 번째 사람에게 나머지 부여
        Collections.shuffle(members);

        List<SplitMemberResponse> splits = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int amount = (i == 0) ? base + remainder : base;
            splits.add(new SplitMemberResponse(members.get(i), amount));
        }

        return ResponseEntity.ok(
                ApiResponse.success("금액 분배가 완료되었습니다.",
                        new SplitResponse(total, count, splits))
        );
    }
}
