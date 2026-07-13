package com.mog.project.domain.summary.dto.response;

import com.mog.project.domain.meeting.dto.response.MenuItemResponse;
import com.mog.project.domain.meeting.dto.response.PlaceResponse;

import java.util.List;

public record SummaryRecordResponse(
        Integer seq,
        PlaceResponse place,
        String memo,
        Integer totalCost,
        List<SummaryParticipantResponse> participants,

        List<MenuItemResponse> menuItems
) {
}
