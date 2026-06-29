package com.mog.project.domain.groups.dto.response;

import java.time.LocalDateTime;

public record GroupCreateResponse(
    Long groupId,
    String groupName,
    String inviteCode,
    String kakaoShareUrl,
    LocalDateTime createdAt
) {}
