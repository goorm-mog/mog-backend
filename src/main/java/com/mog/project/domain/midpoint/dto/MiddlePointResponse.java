package com.mog.project.domain.midpoint.dto;
 
import com.mog.project.domain.midpoint.entity.MiddlePoint;
import com.mog.project.domain.midpoint.entity.TravelTime;
 
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
 
public record MiddlePointResponse(
        Long middlePointId,
        Long roomId,
        BigDecimal latitude,
        BigDecimal longitude,
        LocalDateTime calculatedAt,
        List<TravelTimeItem> travelTimes
) {
    public record TravelTimeItem(
            Long userId,
            Integer durationMinutes,
            String transportType
    ) {}
 
    public static MiddlePointResponse from(MiddlePoint middlePoint, List<TravelTime> travelTimes) {
        List<TravelTimeItem> items = travelTimes.stream()
                .map(t -> new TravelTimeItem(
                        t.getUserId(),
                        t.getDurationMinutes(),
                        t.getTransportType()
                ))
                .toList();
 
        return new MiddlePointResponse(
                middlePoint.getId(),
                middlePoint.getRoomId(),
                middlePoint.getLatitude(),
                middlePoint.getLongitude(),
                middlePoint.getCalculatedAt(),
                items
        );
    }
 
    // 소요시간 없이 조회만 할 때 사용
    public static MiddlePointResponse from(MiddlePoint middlePoint) {
        return new MiddlePointResponse(
                middlePoint.getId(),
                middlePoint.getRoomId(),
                middlePoint.getLatitude(),
                middlePoint.getLongitude(),
                middlePoint.getCalculatedAt(),
                List.of()
        );
    }
}