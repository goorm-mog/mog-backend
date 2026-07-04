package com.mog.project.domain.meeting.dto.response;

public record OcrItemResponse(
        String name, // 메뉴명
        Integer count, // 갯수
        Integer price // 가격
) {
}
