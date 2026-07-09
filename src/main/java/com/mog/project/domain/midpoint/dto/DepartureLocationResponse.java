package com.mog.project.domain.midpoint.dto;
 
import com.mog.project.domain.midpoint.entity.DepartureLocation;
 
import java.math.BigDecimal;
import java.time.LocalDateTime;
 
public record DepartureLocationResponse(
        Long departureId,
        Long roomId,
        Long userId,
        String placeName,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String transportType,
        LocalDateTime updatedAt
) {
    public static DepartureLocationResponse from(DepartureLocation departure) {
        return new DepartureLocationResponse(
                departure.getId(),
                departure.getRoomId(),
                departure.getUserId(),
                departure.getPlaceName(),
                departure.getAddress(),
                departure.getLatitude(),
                departure.getLongitude(),
                departure.getTransportType(),
                departure.getUpdatedAt()
        );
    }
}
 