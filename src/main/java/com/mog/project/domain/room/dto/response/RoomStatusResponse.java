package com.mog.project.domain.room.dto.response;


import com.mog.project.domain.room.entity.RoomStatus;
import java.util.List;

public record RoomStatusResponse (
    Long roomId,
    String roomName,
    RoomStatus status,
    int currentStep,
    List<MemberInfo> members
) {
    public record MemberInfo(
        Long userId,
        String nickname,
        boolean isJoined
    ) {}
}
