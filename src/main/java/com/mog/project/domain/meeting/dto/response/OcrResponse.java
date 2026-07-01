package com.mog.project.domain.meeting.dto.response;

import java.util.List;

public record OcrResponse(
        String storeName, // 가게명
        Integer totalAmount, // 총 금액
        List<OcrItemResponse> items // 메뉴 목록
) {
    public static OcrResponse empty() {
        return new OcrResponse(
                null, 0, List.of()
        );
    }
}
