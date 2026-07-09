package com.mog.project.domain.midpoint.dto;
 
import com.mog.project.domain.midpoint.entity.ConfirmedPlace;
 
import java.math.BigDecimal;
import java.time.LocalDateTime;
 
public record ConfirmedPlaceResponse(
        Long confirmedPlaceId,
        Long roomId,
        String kakaoPlaceId,
        String placeName,
        String address,
        String category,
        BigDecimal latitude,
        BigDecimal longitude,
        LocalDateTime confirmedAt
) {
    public static ConfirmedPlaceResponse from(ConfirmedPlace confirmedPlace) {
        return new ConfirmedPlaceResponse(
                confirmedPlace.getId(),
                confirmedPlace.getRoomId(),
                confirmedPlace.getKakaoPlaceId(),
                confirmedPlace.getPlaceName(),
                confirmedPlace.getAddress(),
                confirmedPlace.getCategory(),
                confirmedPlace.getLatitude(),
                confirmedPlace.getLongitude(),
                confirmedPlace.getConfirmedAt()
        );
    }
}
 