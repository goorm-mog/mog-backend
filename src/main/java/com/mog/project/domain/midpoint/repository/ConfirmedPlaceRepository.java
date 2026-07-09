package com.mog.project.domain.midpoint.repository;

import com.mog.project.domain.midpoint.entity.ConfirmedPlace;
import org.springframework.data.jpa.repository.JpaRepository;
 
import java.util.Optional;
 
public interface ConfirmedPlaceRepository extends JpaRepository<ConfirmedPlace, Long> {
 
    // 방의 확정 장소 조회 (room_id UNIQUE → 최대 1건)
    Optional<ConfirmedPlace> findByRoomId(Long roomId);
 
    // 확정 장소 존재 여부 확인 (upsert 처리 시 사용)
    boolean existsByRoomId(Long roomId);
}
 