package com.mog.project.meeting.dto.response;

import com.mog.project.meeting.entity.MeetingMemberCost;
import com.mog.project.meeting.entity.MeetingRecord;

import java.time.LocalDateTime;
import java.util.List;

public record MeetingRecordResponse (
        // 차수 기록 ID
        Long recordId,

        // 차수 번호
        Integer seq,

        // 장소명
        String placeName,

        // 메모
        String memo,

        // 차수의 총 금액 - participants의 amount 합산
        Integer totalCost,

        // 참여 목록
        List<ParticipantResponse> participants,

        // 생성 일시
        LocalDateTime createdAt
) {
    public static MeetingRecordResponse from(MeetingRecord record, List<MeetingMemberCost> costs) {

        // costs를 ParticipantResponse의 형태로 형변환
        List<ParticipantResponse> participants = costs.stream()
                .map(ParticipantResponse::from)
                .toList();

        // totalCost, 각각의 비용을 합산
        int totalCost = costs.stream()
                .mapToInt(MeetingMemberCost::getAmount)
                .sum();

        return new MeetingRecordResponse(
                record.getId(),
                record.getSeq(),
                record.getPlaceName(),
                record.getMemo(),
                totalCost,
                participants,
                record.getCreatedAt()
        );
    }
}
