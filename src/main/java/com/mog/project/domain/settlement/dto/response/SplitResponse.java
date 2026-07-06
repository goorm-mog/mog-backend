package com.mog.project.domain.settlement.dto.response;

import java.util.List;

public record SplitResponse(
        Integer totalAmount,
        Integer memberCount,
        List<SplitMemberResponse> splits
) {
}
