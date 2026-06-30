package com.mog.project.domain.settlement.repository;

import com.mog.project.domain.settlement.entity.MemberSettlement;
import com.mog.project.domain.settlement.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberSettlementRepository extends JpaRepository<MemberSettlement, Long> {
    // 특정 정산에 속한 멤버별 정산 목록 조회
    List<MemberSettlement> findBySettlement(Settlement settlement);


    // 정산 재계산 시 기존 멤버별 정산 결과 삭제
    void deleteBySettlement(Settlement settlement);


}
