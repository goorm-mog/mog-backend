package com.mog.project.domain.room.dto.response;

import com.mog.project.domain.room.entity.RoomStatus;
import java.time.LocalDateTime;

public record RoomCloseResponse(
    Long roomId,
    RoomStatus status,
    LocalDateTime deletedAt
) {}
