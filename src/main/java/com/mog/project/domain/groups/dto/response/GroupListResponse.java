package com.mog.project.domain.groups.dto.response;

import java.util.List;

public record GroupListResponse (
    List<GroupItemResponse> groups
) {
    public record GroupItemResponse(
        Long groupId,
        String groupName,
        int memberCount
    ) {}
}
