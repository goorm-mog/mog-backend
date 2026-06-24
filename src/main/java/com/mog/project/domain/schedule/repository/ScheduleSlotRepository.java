package com.mog.project.domain.schedule.repository;

import com.mog.project.domain.schedule.entity.ScheduleSlot;
import org.springframework.data.jpa.repository.JpaRepository;
 
import java.util.List;

public interface ScheduleSlotRepository extends JpaRepository<ScheduleSlot, Long> {

    // 방의 모든 슬롯 조회 (슬롯 목록 + 투표 현황 조회 시 사용)
    List<ScheduleSlot> findByRoomId(Long roomId);

    // 방의 슬롯 전체 삭제 (슬롯 재등록 시 사용)
    void deleteByRoomId(Long roomId);
    
}
