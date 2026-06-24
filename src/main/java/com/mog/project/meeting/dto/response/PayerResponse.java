package com.mog.project.meeting.dto.response;

import com.mog.project.meeting.dto.request.PayerRequest;
import com.mog.project.meeting.entity.MeetingRecord;

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
    public static PayerResponse from(MeetingRecord record) {
        if (record.getPayerRoomMemberId() == null) return null;

        return new PayerResponse(
                record.getPayerRoomMemberId(),
                "멤버" + record.getPayerRoomMemberId(),
                record.getPayerBankName(),
                record.getPayerAccountNumber()
        );
    }

}
