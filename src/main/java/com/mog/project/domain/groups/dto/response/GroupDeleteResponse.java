package com.mog.project.domain.groups.dto.response;

import java.time.LocalDateTime;

public record GroupDeleteResponse(
    Long groupId,
    LocalDateTime deletedAt
) {}
