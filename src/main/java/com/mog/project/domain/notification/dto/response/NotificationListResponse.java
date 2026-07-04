package com.mog.project.domain.notification.dto.response;

import java.util.List;

public record NotificationListResponse(
        Integer unreadCount,
        List<NotificationResponse> notifications
) {
}
