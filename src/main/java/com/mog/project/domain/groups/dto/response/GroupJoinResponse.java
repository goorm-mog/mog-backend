package com.mog.project.domain.groups.dto.response;

import com.mog.project.domain.groups.entity.GroupMemberRole;

public record GroupJoinResponse (
    Long groupId,
    String groupName,
    GroupMemberRole role
) {}
