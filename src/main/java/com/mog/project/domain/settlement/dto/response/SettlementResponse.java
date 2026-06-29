package com.mog.project.domain.settlement.dto.response;

import com.mog.project.domain.settlement.entity.Settlement;

import java.time.LocalDateTime;
import java.util.List;

public record SettlementResponse(
        Long settlementId,
        Integer totalCost,
        Boolean isConfirmed,
        LocalDateTime confirmedAt,
        List<MemberSettlementResponse> memberSettlements,
        LocalDateTime createdAt
) {
    public static SettlementResponse from(
            Settlement settlement,
            List<MemberSettlementResponse> memberSettlements
    ) {
        return new SettlementResponse(
                settlement.getId(),
                settlement.getTotalCost(),
                settlement.getIsConfirmed(),
                settlement.getConfirmedAt(),
                memberSettlements,
                settlement.getCreatedAt()
        );
    }
}
