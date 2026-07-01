package com.mog.project.domain.meeting.dto.response;

import com.mog.project.domain.meeting.entity.MeetingMemberCost;

import java.util.Map;

public record ParticipantResponse (
        // 방 멤버 ID
        Long roomMemberId,

        // 닉네임
        String nickname,

        // 개인 부담 금액
        Integer amount
) {
    public static ParticipantResponse from(MeetingMemberCost cost, Map<Long, String> nicknameMap) {
        return new ParticipantResponse(
                cost.getRoomMemberId(),
                nicknameMap.get(cost.getRoomMemberId()),
                cost.getAmount()
        );
    }
}
