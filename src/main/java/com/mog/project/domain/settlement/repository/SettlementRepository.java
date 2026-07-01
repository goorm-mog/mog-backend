package com.mog.project.domain.settlement.repository;

import com.mog.project.domain.settlement.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    // roomId를 통해 정산을 조회
    Optional<Settlement> findByRoomId(Long roomId);

}
