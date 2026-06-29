package com.mog.project.domain.meeting.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ParticipantRequest(

        // 요청에 받는 방 멤버 ID
        @NotNull(message = "방 멤버 ID는 필수입니다.")
        Long roomMemberId,

        // 개인부담 금액
        @NotNull(message = "금액은 필수입니다.")
        @Min(value = 0, message = "금액은 0 이상이어야 합니다.")
        Integer amount

) {
}
