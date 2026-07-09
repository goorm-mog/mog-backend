package com.mog.project.domain.settlement.entity;


import com.mog.project.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "member_settlement",
        uniqueConstraints = @UniqueConstraint(columnNames = {"settlement_id", "room_member_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberSettlement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_id", nullable = false)
    private Settlement settlement;

    @Column(name = "room_member_id", nullable = false)
    private Long roomMemberId;

    @Column(name = "total_amount", nullable = false)
    private Integer totalAmount; // 해당 맴버의 최종 부담액


    @Builder
    public MemberSettlement(Settlement settlement, Long roomMemberId, Integer totalAmount) {
        this.settlement = settlement;
        this.roomMemberId = roomMemberId;
        this.totalAmount = totalAmount;
    }

    void assignSettlement(Settlement settlement) {
        this.settlement = settlement;
    }

}
