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
            String nickname,
            List<SettlementDetailResponse> detail
    ) {
        return new MemberSettlementResponse(
                memberSettlement.getRoomMemberId(),
                nickname,
                memberSettlement.getTotalAmount(),
                detail
        );
    }
}
