package com.mog.project.domain.meeting.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PlaceRequest(
        @NotBlank(message = "장소 이름은 필수입니다.")
        String name,
        String address
) {
}
