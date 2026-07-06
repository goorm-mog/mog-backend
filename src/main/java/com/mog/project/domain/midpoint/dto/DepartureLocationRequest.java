package com.mog.project.domain.midpoint.dto;
 
import java.math.BigDecimal;
 
public record DepartureLocationRequest(
        String placeName,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String transportType  // WALK | CAR | PUBLIC
) {}
 