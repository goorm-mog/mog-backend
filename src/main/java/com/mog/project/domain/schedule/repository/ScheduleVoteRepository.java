package com.mog.project.domain.schedule.repository;

import com.mog.project.domain.schedule.entity.ScheduleVote;
import org.springframework.data.jpa.repository.JpaRepository;
 
import java.util.List;

public interface ScheduleVoteRepository extends JpaRepository<ScheduleVote, Long> {
    // 특정 슬롯에 투표한 목록 조회 (투표 현황 집계 시 사용)
    List<ScheduleVote> findAllBySlotId(Long slotId);
 
    // 방의 모든 투표 조회 (슬롯 목록과 함께 현황 조회 시 사용)
    List<ScheduleVote> findAllByRoomId(Long roomId);
 
    // 특정 유저가 특정 슬롯에 투표했는지 확인 (토글 처리 시 사용)
    boolean existsBySlotIdAndUserId(Long slotId, Long userId);
 
    // 특정 유저의 특정 슬롯 투표 삭제 (토글 - 취소 시 사용)
    void deleteBySlotIdAndUserId(Long slotId, Long userId);
 
    // 특정 유저가 방에서 투표한 슬롯 ID 목록 조회 (투표 응답 반환 시 사용)
    List<ScheduleVote> findAllByRoomIdAndUserId(Long roomId, Long userId);
}
