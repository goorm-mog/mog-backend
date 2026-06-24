package com.mog.project.schedule.entity;
 
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
 
import java.time.LocalDateTime;
 
@Entity
@Table(name = "schedule_votes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"slot_id", "user_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleVote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vote_id")
    private Long Id;

    @Column(name = "slot_id", nullable = false)
    private Long slotId;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;


    @Builder
    public ScheduleVote(Long slotId, Long roomId, Long userId) {
        this.slotId = slotId;
        this.roomId = roomId;
        this.userId = userId;
        this.createdAt = LocalDateTime.now();
    }
}