package com.mog.project.domain.summary.entity;

import com.mog.project.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "summary_card")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SummaryCard extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 방 하나에 카드 하나
    @Column(name = "room_id", nullable = false, unique = true)
    private Long roomId;

    @Column(name = "settlement_id", nullable = false)
    private Long settlementId;

    // 카드 이미지 S3 URL
    @Column(name = "s3_url", nullable = false, length = 500)
    private String s3Url;

    @Builder
    public SummaryCard(Long roomId, Long settlementId, String s3Url) {
        this.roomId = roomId;
        this.settlementId = settlementId;
        this.s3Url = s3Url;
    }

    // 카드가 이미 존재할 때 이미지만 교체
    public void updateS3Url(String s3Url) {
        this.s3Url = s3Url;
    }

}
