package com.mog.project.meeting.dto.response;

import com.mog.project.meeting.entity.MeetingMemberCost;

public record ParticipantResponse (
        // 방 멤버 ID
        Long roomMemberId,

        // 닉네임
        String nickname,

        // 개인 부담 금액
        Integer amount
) {
    public static ParticipantResponse from(MeetingMemberCost cost) {
        return new ParticipantResponse(
                cost.getRoomMemberId(),
                "멤버" + cost.getRoomMemberId(), // 임시 닉네임으로 roommemberid에 멤버를 붙이는 형태로 임시 구성
                cost.getAmount()
        );
    }
}
