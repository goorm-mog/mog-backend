package com.mog.project.domain.midpoint.dto;
 
import java.math.BigDecimal;
import java.util.List;
 
public record NearbyPlaceResponse(
        BigDecimal midpointLatitude,
        BigDecimal midpointLongitude,
        List<PlaceItem> places
) {
    public record PlaceItem(
            String kakaoPlaceId,
            String placeName,
            String category,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            int distance
    ) {}
}
 