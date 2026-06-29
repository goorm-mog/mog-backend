package com.mog.project.domain.meeting.repository;

import com.mog.project.domain.meeting.entity.MeetingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MeetingRecordRepository  extends JpaRepository<MeetingRecord, Long> {

    // 전체 조회이며, 만남 기록에 대한 전체 기록이 나옴
    List<MeetingRecord> findByRoomId(Long roomId);

    // 특정 기록을 수정, 삭제하기 위해 사용
    Optional<MeetingRecord> findByIdAndRoomId(Long id, Long roomId);

    // 새 차수 추가 시 seq를 자동 부여하고 현재 가장 높은 seq을 가져와서 차수 생성
    @Query("SELECT COALESCE(MAX(m.seq), 0) FROM MeetingRecord m WHERE m.roomId = :roomId")
    int findMaxSeqByRoomId(@Param("roomId") Long roomId);

    // 중간에 seq이 삭제가 되었을 때 그 뒤에 있는 차수들이 한 번씩 -1이 되서 당겨짐
    @Modifying
    @Query("UPDATE MeetingRecord m SET m.seq = m.seq - 1 WHERE m.roomId = :roomId AND m.seq > :seq")
    void decreaseSeqAfter(@Param("roomId") Long roomId, @Param("seq") int seq);

}
