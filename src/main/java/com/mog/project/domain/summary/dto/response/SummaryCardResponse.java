package com.mog.project.domain.summary.dto.response;

import java.time.LocalDate;
import java.util.List;

public record SummaryCardResponse(
        Long roomId,
        LocalDate confirmedDate,
        String confirmedPlace,
        Integer totalMemberCount,
        List<String> members,
        List<String> photos,
        List<SummaryRecordResponse> records,
        SummarySettlementResponse settlement,
        String cardImageUrl
) {
}
