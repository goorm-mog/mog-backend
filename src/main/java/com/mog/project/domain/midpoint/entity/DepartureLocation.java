package com.mog.project.domain.midpoint.entity;
 
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
 
import java.math.BigDecimal;
import java.time.LocalDateTime;
 
@Entity
@Table(
    name = "departure_locations",
    uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "user_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DepartureLocation {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "departure_id")
    private Long id;
 
    @Column(name = "room_id", nullable = false)
    private Long roomId;
 
    @Column(name = "user_id", nullable = false)
    private Long userId;
 
    @Column(name = "place_name", nullable = false, length = 255)
    private String placeName;
 
    @Column(name = "address", nullable = false, length = 500)
    private String address;
 
    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;
 
    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;
 
    @Column(name = "transport_type", nullable = false, length = 20)
    private String transportType;  // WALK | CAR | PUBLIC
 
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
 
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
 
    @Builder
    public DepartureLocation(Long roomId, Long userId, String placeName, String address,
                              BigDecimal latitude, BigDecimal longitude, String transportType) {
        this.roomId = roomId;
        this.userId = userId;
        this.placeName = placeName;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.transportType = transportType;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
 
    // 출발지 수정 (PATCH)
    public void update(String placeName, String address, BigDecimal latitude,
                       BigDecimal longitude, String transportType) {
        this.placeName = placeName;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.transportType = transportType;
        this.updatedAt = LocalDateTime.now();
    }
}