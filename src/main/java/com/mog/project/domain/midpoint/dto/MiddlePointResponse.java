package com.mog.project.domain.midpoint.dto;
 
import com.mog.project.domain.midpoint.entity.MiddlePoint;
 
import java.math.BigDecimal;
import java.time.LocalDateTime;
 
public record MiddlePointResponse(
        Long middlePointId,
        Long roomId,
        BigDecimal latitude,
        BigDecimal longitude,
        LocalDateTime calculatedAt
) {
    public static MiddlePointResponse from(MiddlePoint middlePoint) {
        return new MiddlePointResponse(
                middlePoint.getId(),
                middlePoint.getRoomId(),
                middlePoint.getLatitude(),
                middlePoint.getLongitude(),
                middlePoint.getCalculatedAt()
        );
    }
}
 