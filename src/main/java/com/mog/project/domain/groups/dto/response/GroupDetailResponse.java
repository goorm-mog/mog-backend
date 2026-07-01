package com.mog.project.domain.groups.dto.response;

import com.mog.project.domain.groups.entity.GroupMemberRole;
import com.mog.project.domain.room.entity.RoomStatus;
import java.time.LocalDateTime;
import java.util.List;

public record GroupDetailResponse(
    Long groupId,
    String groupName,
    String inviteCode,
    GroupMemberRole myRole,
    List<MemberInfo> members,
    List<RoomInfo> rooms
) {
    public record MemberInfo(
        Long userId,
        String nickname,
        GroupMemberRole role
    ) {}

    public record RoomInfo(
        Long roomId,
        String roomName,
        RoomStatus status,
        LocalDateTime promiseDate
    ) {}
}
