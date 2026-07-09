package com.mog.project.domain.settlement.dto.response;

import com.mog.project.domain.meeting.dto.response.PayerResponse;
import com.mog.project.domain.meeting.entity.MeetingMemberCost;
import com.mog.project.domain.meeting.entity.MeetingRecord;

import java.util.Map;

// 멤버별 정산 안에 들어가는 차수별 상세 금액
public record SettlementDetailResponse (
        Integer seq, // 차수
        String placeName, // 장소 이름
        Integer amount, // 가격
        PayerResponse payer // 결제자
) {
    public static SettlementDetailResponse from(MeetingMemberCost cost, Map<Long, String> nicknameMap) {
        MeetingRecord record = cost.getMeetingRecord();

        return new SettlementDetailResponse(
                record.getSeq(),
                record.getPlaceName(),
                cost.getAmount(),
                PayerResponse.from(record, nicknameMap)
        );
    }
}
