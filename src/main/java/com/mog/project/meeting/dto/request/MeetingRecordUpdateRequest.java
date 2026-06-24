package com.mog.project.meeting.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

// 차수 수정 시 받는 DTO
public record MeetingRecordUpdateRequest(

        // 장소명
        String placeName,

        // 메모
        @Size(max = 200, message = "메모는 200자 이하여야 합니다.")
        String memo,

        @Valid
        PayerRequest payer,

        // 참여 멤버 목록
        @Valid
        List<ParticipantRequest> participants

) {
}
