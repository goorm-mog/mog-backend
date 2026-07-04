package com.mog.project.domain.summary.dto.response;

import java.util.List;

public record SummaryRecordResponse(
        Integer seq,
        String placeName,
        String memo,
        Integer totalCost,
        List<SummaryParticipantResponse> participants
) {
}
