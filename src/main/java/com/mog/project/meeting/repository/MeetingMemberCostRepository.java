package com.mog.project.meeting.repository;

import com.mog.project.meeting.entity.MeetingMemberCost;
import com.mog.project.meeting.entity.MeetingRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MeetingMemberCostRepository extends JpaRepository<MeetingMemberCost, Long> {

    // 전체 차수 조회 시 각 차수의 참여자 목록을 조회
    // 차수별로 따로 조회할 경우 너무 많은 쿼리를 실행하기 때문에, 전체 차수를 조회한 뒤, 참여자 목록을 조회해서 가져올 예정
    List<MeetingMemberCost> findByMeetingRecordIn(List<MeetingRecord> records);

    // 차수 수정 시 기존 참여자를 삭제
    void deleteByMeetingRecord(MeetingRecord meetingRecord);
}
