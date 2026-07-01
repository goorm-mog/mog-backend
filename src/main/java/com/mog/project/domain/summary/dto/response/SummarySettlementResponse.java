package com.mog.project.domain.summary.dto.response;

import java.util.List;

public record SummarySettlementResponse(
        Integer totalCost,
        List<SummaryMemberTotalResponse> memberTotals
) {
}
