package com.mog.project.domain.notification.dto.response;

import com.mog.project.domain.notification.entity.Notification;
import com.mog.project.domain.notification.entity.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse (
        Long notificationId,
        NotificationType type,
        String message,
        Long roomId,
        Boolean isRead,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getMessage(),
                notification.getRoomId(),
                notification.getIsRead(),
                notification.getCreatedAt()
        );
    }
}
