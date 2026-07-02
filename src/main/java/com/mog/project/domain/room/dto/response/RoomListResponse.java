package com.mog.project.domain.room.dto.response;

import com.mog.project.domain.room.entity.RoomStatus;
import java.time.LocalDateTime;
import java.util.List;

public record RoomListResponse (
    List<RoomInfo> rooms
) {
    public record RoomInfo(
        Long RoomId,
        String roomName,
        RoomStatus status,
        LocalDateTime createdAt
    ) {}
}
