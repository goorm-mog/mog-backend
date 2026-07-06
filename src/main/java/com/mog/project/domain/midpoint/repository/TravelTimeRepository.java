package com.mog.project.domain.midpoint.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mog.project.domain.midpoint.entity.TravelTime;
 
public interface TravelTimeRepository extends JpaRepository<TravelTime, Long> {
 
    // 방의 모든 소요시간 조회
    List<TravelTime> findAllByRoomId(Long roomId);
 
    // 특정 유저의 소요시간 조회 (upsert 처리 시 사용)
    Optional<TravelTime> findByRoomIdAndUserId(Long roomId, Long userId);
}