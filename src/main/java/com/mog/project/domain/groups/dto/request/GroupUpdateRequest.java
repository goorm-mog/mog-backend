package com.mog.project.domain.groups.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GroupUpdateRequest (
    @NotBlank(message = "그룹 이름은 필수입니다.")
    @Size(min = 1, max = 20, message = "그룹 이름은 1자 이상 20자 이하로 입력해주세요.")
    String groupName
) {}
