package com.mog.project.domain.settlement.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SplitRequest(
        @NotNull
        @Min(value = 1, message = "총 금액은 1원 이상이어야 합니다.")
        Integer totalAmount,

        @NotEmpty(message = "멤버는 1명 이상이어야 합니다.")
        List<String> members
) {
}
