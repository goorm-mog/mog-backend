package com.mog.project.domain.room.entity;

import com.mog.project.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "room_members",
    uniqueConstraints = @UniqueConstraint(
        name = "ux_roommembers_room_user",
        columnNames = {"room_id", "user_id"}
    )
)

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long roomMemberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private boolean isJoined;

    @Builder
    public RoomMember(Room room, User user, boolean isJoined) {
        this.room = room;
        this.user = user;
        this.isJoined = isJoined;
    }
}
