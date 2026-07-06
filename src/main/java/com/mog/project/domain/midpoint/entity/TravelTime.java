package com.mog.project.domain.midpoint.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
 
import java.time.LocalDateTime;
 
@Entity
@Table(
    name = "travel_times",
    uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "user_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TravelTime {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "travel_time_id")
    private Long id;
 
    @Column(name = "room_id", nullable = false)
    private Long roomId;
 
    @Column(name = "user_id", nullable = false)
    private Long userId;
 
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;
 
    @Column(name = "transport_type", nullable = false, length = 20)
    private String transportType;  // WALK | CAR | PUBLIC
 
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
 
    @Builder
    public TravelTime(Long roomId, Long userId, Integer durationMinutes, String transportType) {
        this.roomId = roomId;
        this.userId = userId;
        this.durationMinutes = durationMinutes;
        this.transportType = transportType;
        this.createdAt = LocalDateTime.now();
    }
 
    // 재계산 시 업데이트
    public void update(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }
}