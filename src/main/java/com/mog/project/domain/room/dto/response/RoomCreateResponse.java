package com.mog.project.domain.room.dto.response;

import com.mog.project.domain.room.entity.RoomStatus;
import java.time.LocalDateTime;

public record RoomCreateResponse (
    Long roomId,
    Long groupId,
    String roomName,
    RoomStatus status,
    Long creatorId,
    LocalDateTime createdAt
) {}