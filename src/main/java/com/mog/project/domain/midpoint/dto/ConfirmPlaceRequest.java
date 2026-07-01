package com.mog.project.domain.midpoint.dto;
 
import java.math.BigDecimal;
 
public record ConfirmPlaceRequest(
        String kakaoPlaceId,
        String placeName,
        String address,
        String category,  // FD6 | CE7 | CT1 | AT4 | SW8
        BigDecimal latitude,
        BigDecimal longitude
) {}
 