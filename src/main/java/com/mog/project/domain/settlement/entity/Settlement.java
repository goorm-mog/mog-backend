package com.mog.project.domain.settlement.entity;

import com.mog.project.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "settlement",
        uniqueConstraints = @UniqueConstraint(columnNames = "room_id")
)
@Getter
@NoArgsConstructor
public class Settlement extends BaseEntity {

    // 정산 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "created_by", nullable = false)
    private Long createdBy; // 정산을 요청한 room_member_id

    @Column(name = "total_cost", nullable = false)
    private Integer totalCost; // 방 전체 소비 금액 합계

    @Column(name = "is_confirmed", nullable = false)
    private Boolean isConfirmed = false;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;


    @OneToMany(mappedBy = "settlement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberSettlement> memberSettlements = new ArrayList<>();

    @Builder
    public Settlement(Long roomId, Long createdBy, Integer totalCost) {
        this.roomId = roomId;
        this.createdBy = createdBy;
        this.totalCost = totalCost;
    }

    public void confirm() {
        this.isConfirmed = true;
        this.confirmedAt = LocalDateTime.now();
    }

    public void updateTotalCost(Integer totalCost) {
        this.totalCost = totalCost;
    }

    public void clearMemberSettlements() {
        this.memberSettlements.clear();
    }

    public void addMemberSettlement(MemberSettlement memberSettlement) {
        this.memberSettlements.add(memberSettlement);
    }


}
