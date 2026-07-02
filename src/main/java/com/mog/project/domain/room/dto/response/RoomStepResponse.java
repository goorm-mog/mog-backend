package com.mog.project.domain.room.dto.response;

import com.mog.project.domain.room.entity.RoomStatus;
import java.time.LocalDateTime;

public record RoomStepResponse(
    Long roomId,
    RoomStatus currentStatus,
    LocalDateTime updatedAt
) {}
