package com.mog.project.domain.notification.entity;

import com.mog.project.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private NotificationType type;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Builder
    public Notification(Long userId, NotificationType type, String message, Long roomId) {
        this.userId = userId;
        this.type = type;
        this.message = message;
        this.roomId = roomId;
        this.isRead = false;
    }

    public void markAsRead() {
        this.isRead = true;
    }





}
