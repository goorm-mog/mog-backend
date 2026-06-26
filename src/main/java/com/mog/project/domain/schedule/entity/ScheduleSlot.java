package com.mog.project.domain.schedule.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "schedule_slot",
    uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "slot_date", "slot_time"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleSlot{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "slot_id")
    private Long Id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "slot_date", nullable = false)
    private LocalDate slotDate;

    @Column(name = "slot_time", nullable = false)
    private LocalTime slotTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;


    @Builder
    public ScheduleSlot(Long roomId, LocalDate slotDate, LocalTime slotTime) {
        this.roomId = roomId;
        this.slotDate = slotDate;
        this.slotTime = slotTime;
        this.createdAt = LocalDateTime.now();
    }
}