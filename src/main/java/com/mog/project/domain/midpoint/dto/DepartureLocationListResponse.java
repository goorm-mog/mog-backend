package com.mog.project.domain.midpoint.dto;
 
import com.mog.project.domain.midpoint.entity.DepartureLocation;
 
import java.math.BigDecimal;
import java.util.List;
 
public record DepartureLocationListResponse(
        Long roomId,
        int submittedCount,
        List<DepartureItem> departures
) {
    public record DepartureItem(
            Long departureId,
            Long userId,
            String placeName,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            String transportType
    ) {}
 
    public static DepartureLocationListResponse from(Long roomId, List<DepartureLocation> departures) {
        List<DepartureItem> items = departures.stream()
                .map(d -> new DepartureItem(
                        d.getId(),
                        d.getUserId(),
                        d.getPlaceName(),
                        d.getAddress(),
                        d.getLatitude(),
                        d.getLongitude(),
                        d.getTransportType()
                ))
                .toList();
 
        return new DepartureLocationListResponse(roomId, items.size(), items);
    }
}
 