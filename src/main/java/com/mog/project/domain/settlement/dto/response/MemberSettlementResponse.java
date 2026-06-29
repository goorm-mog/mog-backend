package com.mog.project.domain.settlement.dto.response;

import com.mog.project.domain.settlement.entity.MemberSettlement;

import java.util.List;

// 정산 응답의 멤버별 블록을 표현하는 DTO
public record MemberSettlementResponse(
        Long roomMemberId,
        String nickname,
        Integer totalAmount,
        List<SettlementDetailResponse> detail
) {
    public static MemberSettlementResponse from(
            MemberSettlement memberSettlement,
            List<SettlementDetailResponse> detail
    ) {
        Long roomMemberId = memberSettlement.getRoomMemberId();

        return new MemberSettlementResponse(
                roomMemberId,
                "멤버" + roomMemberId,
                memberSettlement.getTotalAmount(),
                detail
        );
    }
}
