package com.mog.project.domain.groups.dto.request;

import jakarta.validation.constraints.NotBlank;

public record GroupJoinRequest (
    @NotBlank(message = "초대 코드는 필수입니다.")
    String inviteCode
) {}
