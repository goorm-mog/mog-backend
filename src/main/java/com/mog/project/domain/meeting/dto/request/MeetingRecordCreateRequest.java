package com.mog.project.domain.meeting.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record MeetingRecordCreateRequest(
        @NotBlank(message = "장소명은 필수입니다.")
        String placeName,

        @Size(max = 200, message = "메모는 200자 이하여야 합니다.")
        String memo,

        // 결제자 정보
        @Valid
        PayerRequest payer,

        @Valid // 리스트 안에 ParticipantRequest 각각에 검증
        @NotEmpty(message = "참여 멤버는 최소 1명이어야 합니다.")
        List<ParticipantRequest> participants
) {
}
