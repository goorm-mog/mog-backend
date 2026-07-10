package com.mog.project.domain.meeting.repository;

import com.mog.project.domain.meeting.entity.MeetingMenuItem;
import com.mog.project.domain.meeting.entity.MeetingRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MeetingMenuItemRepository extends JpaRepository<MeetingMenuItem, Long> {

    // 여러 차수의 메뉴 아이템을 한 번에 조회 (전체 목록 조회시)
    List<MeetingMenuItem> findByMeetingRecordIn(List<MeetingRecord> records);

    // 특정 차수의 메뉴 아이템을 삭제
    void deleteByMeetingRecord(MeetingRecord meetingRecord);
}
