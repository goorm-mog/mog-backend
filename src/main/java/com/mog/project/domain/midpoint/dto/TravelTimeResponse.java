package com.mog.project.domain.midpoint.dto;

import com.mog.project.domain.midpoint.entity.TravelTime;
 
import java.util.List;
 
public record TravelTimeResponse(
        Long roomId,
        List<TravelTimeItem> travelTimes
) {
    public record TravelTimeItem(
            Long userId,
            Integer durationMinutes,
            String transportType
    ) {}
 
    public static TravelTimeResponse from(Long roomId, List<TravelTime> travelTimes) {
        List<TravelTimeItem> items = travelTimes.stream()
                .map(t -> new TravelTimeItem(
                        t.getUserId(),
                        t.getDurationMinutes(),
                        t.getTransportType()
                ))
                .toList();
 
        return new TravelTimeResponse(roomId, items);
    }
}