package com.mog.project.domain.meeting.dto.response;

import com.mog.project.domain.meeting.entity.MeetingRecord;

import java.util.Map;

public record PayerResponse(

        // 결제자 방 멤버 ID
        Long roomMemberId,

        // 닉네임, 임시로 멤버 + roomMemberId 형태로 반환
        String nickname,

        // 은행명
        String bankName,

        // 계좌번호
        String accountNumber
) {
    public static PayerResponse from(MeetingRecord record, Map<Long, String> nicknameMap) {
        if (record.getPayerRoomMemberId() == null) return null;

        return new PayerResponse(
                record.getPayerRoomMemberId(),
                nicknameMap.get(record.getPayerRoomMemberId()),
                record.getPayerBankName(),
                record.getPayerAccountNumber()
        );
    }

}
