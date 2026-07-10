package com.mog.project.domain.room.entity;

import com.mog.project.domain.groups.entity.Group;
import com.mog.project.domain.user.entity.User;
import com.mog.project.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Room extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long roomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Column(nullable = false, length = 50)
    private String roomName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RoomStatus status;

    @Column
    private LocalDateTime promiseDate;

    private LocalDateTime deletedAt;

    @Builder
    public Room(Group group, User creator, String roomName, RoomStatus status, LocalDateTime promiseDate) {
        this.group = group;
        this.creator = creator;
        this.roomName = roomName;
        this.status = status;
        this.promiseDate = promiseDate;
    }

    public void updateStatus(RoomStatus status) {
        this.status = status;
    }

    public void close() {
        this.status = RoomStatus.COMPLETED;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}
