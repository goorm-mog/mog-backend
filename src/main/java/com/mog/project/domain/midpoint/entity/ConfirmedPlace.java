package com.mog.project.domain.midpoint.entity;
 
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
 
import java.math.BigDecimal;
import java.time.LocalDateTime;
 
@Entity
@Table(name = "confirmed_places")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConfirmedPlace {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "confirmed_place_id")
    private Long id;
 
    // room_id UNIQUE → 모임방당 확정 장소 최대 1건
    @Column(name = "room_id", nullable = false, unique = true)
    private Long roomId;
 
    @Column(name = "kakao_place_id", nullable = false, length = 50)
    private String kakaoPlaceId;
 
    @Column(name = "place_name", nullable = false, length = 255)
    private String placeName;
 
    @Column(name = "address", nullable = false, length = 500)
    private String address;
 
    @Column(name = "category", length = 50)
    private String category;  // FD6 | CE7 | CT1 | AT4 | SW8
 
    @Column(name = "latitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;
 
    @Column(name = "longitude", nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;
 
    @Column(name = "confirmed_at", nullable = false, updatable = false)
    private LocalDateTime confirmedAt;
 
    @Builder
    public ConfirmedPlace(Long roomId, String kakaoPlaceId, String placeName, String address,
                          String category, BigDecimal latitude, BigDecimal longitude) {
        this.roomId = roomId;
        this.kakaoPlaceId = kakaoPlaceId;
        this.placeName = placeName;
        this.address = address;
        this.category = category;
        this.latitude = latitude;
        this.longitude = longitude;
        this.confirmedAt = LocalDateTime.now();
    }
 
    // 장소 재확정 시 업데이트
    public void update(String kakaoPlaceId, String placeName, String address,
                       String category, BigDecimal latitude, BigDecimal longitude) {
        this.kakaoPlaceId = kakaoPlaceId;
        this.placeName = placeName;
        this.address = address;
        this.category = category;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}