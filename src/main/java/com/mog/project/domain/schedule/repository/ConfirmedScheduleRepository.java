package com.mog.project.domain.schedule.repository;

import com.mog.project.domain.schedule.entity.ConfirmedSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
 
import java.util.Optional;

public interface ConfirmedScheduleRepository extends JpaRepository<ConfirmedSchedule, Long> {
    // 방의 확정 일정 조회 (확정 일정 반환 시 사용)
    Optional<ConfirmedSchedule> findByRoomId(Long roomId);

    // 방의 확정 일정 존재 여부 확인 (upsert 처리 시 사용)
    boolean existsByRoomId(Long roomId);
}
