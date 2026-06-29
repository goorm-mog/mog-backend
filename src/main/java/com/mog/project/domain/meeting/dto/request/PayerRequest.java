package com.mog.project.domain.meeting.dto.request;

public record PayerRequest(
        // 결제자 방 멤버 ID
        Long roomMemberId,

        // 은행명
        String bankName,

        // 계좌번호
        String accountNumber
) {
}
