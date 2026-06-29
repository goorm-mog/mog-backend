package com.mog.project.domain.meeting.dto.response;


import com.mog.project.domain.meeting.entity.MeetingMemberCost;
import com.mog.project.domain.meeting.entity.MeetingRecord;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record MeetingRecordListResponse(
        List<RoomPhotoResponse> photos,
        List<MeetingRecordResponse> records
) {
    public static MeetingRecordListResponse from(
            List<RoomPhotoResponse> photos,
            List<MeetingRecord> records,
            List<MeetingMemberCost> allCosts // 모든 차수의 MeetingMemberCost를 한 번에 조회
    ) {
        // MeetingMemberCost를 meetingRecordId를 기준으로 그룹
        // 각 차수별로 해당하는 cost를 찾기 위해 Map으로 변환
        Map<Long, List<MeetingMemberCost>> costsByRecordId = allCosts.stream()
                .collect(Collectors.groupingBy(cost -> cost.getMeetingRecord().getId()));

        List<MeetingRecordResponse> recordResponses = records.stream()
                .map(record -> MeetingRecordResponse.from(
                        record,
                        costsByRecordId.getOrDefault(record.getId(), List.of())
                ))
                .toList();

        return new MeetingRecordListResponse(photos, recordResponses);

    }

}
