package com.mog.project.domain.midpoint.entity;
 
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
 
import java.math.BigDecimal;
import java.time.LocalDateTime;
 
@Entity
@Table(name = "middle_points")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MiddlePoint {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "middle_point_id")
    private Long id;
 
    // room_id UNIQUE → 모임방당 중간지점 최대 1건
    @Column(name = "room_id", nullable = false, unique = true)
    private Long roomId;
 
    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;
 
    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;
 
    @Column(name = "calculated_at", nullable = false, updatable = false)
    private LocalDateTime calculatedAt;
 
    @Builder
    public MiddlePoint(Long roomId, BigDecimal latitude, BigDecimal longitude) {
        this.roomId = roomId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.calculatedAt = LocalDateTime.now();
    }
 
    // 중간지점 재계산 시 업데이트
    public void update(BigDecimal latitude, BigDecimal longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.calculatedAt = LocalDateTime.now();
    }
}