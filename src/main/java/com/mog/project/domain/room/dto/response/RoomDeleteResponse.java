package com.mog.project.domain.room.dto.response;

import java.time.LocalDateTime;

public record RoomDeleteResponse(
    Long roomId,
    LocalDateTime deletedAt
) {}
