package com.mog.project.domain.summary.repository;

import com.mog.project.domain.summary.entity.SummaryCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SummaryCardRepository extends JpaRepository<SummaryCard, Long> {
    Optional<SummaryCard> findByRoomId(Long roomId);
}
