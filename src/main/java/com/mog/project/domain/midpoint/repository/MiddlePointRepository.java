package com.mog.project.domain.midpoint.repository;

import com.mog.project.domain.midpoint.entity.MiddlePoint;
import org.springframework.data.jpa.repository.JpaRepository;
 
import java.util.Optional;
 
public interface MiddlePointRepository extends JpaRepository<MiddlePoint, Long> {
 
    // 방의 중간지점 조회 (room_id UNIQUE → 최대 1건)
    Optional<MiddlePoint> findByRoomId(Long roomId);
 
    // 중간지점 존재 여부 확인 (upsert 처리 시 사용)
    boolean existsByRoomId(Long roomId);
}
 