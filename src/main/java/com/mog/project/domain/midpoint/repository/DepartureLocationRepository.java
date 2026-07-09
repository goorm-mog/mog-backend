package com.mog.project.domain.midpoint.repository;

import com.mog.project.domain.midpoint.entity.DepartureLocation;
import org.springframework.data.jpa.repository.JpaRepository;
 
import java.util.List;
import java.util.Optional;
 
public interface DepartureLocationRepository extends JpaRepository<DepartureLocation, Long> {
 
    // 방의 모든 출발지 조회 (출발지 목록 조회 시 사용)
    List<DepartureLocation> findAllByRoomId(Long roomId);
 
    // 특정 유저의 출발지 조회 (PATCH 시 사용)
    Optional<DepartureLocation> findByRoomIdAndUserId(Long roomId, Long userId);
 
    // 출발지 등록 여부 확인 (POST 시 중복 체크)
    boolean existsByRoomIdAndUserId(Long roomId, Long userId);
 
    // 방의 출발지 등록 인원 수 조회 (전원 등록 여부 확인 시 사용)
    long countByRoomId(Long roomId);
}
 