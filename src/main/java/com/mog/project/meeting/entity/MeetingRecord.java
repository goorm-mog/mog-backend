package com.mog.project.meeting.entity;

import com.mog.project.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "meeting_record",
        uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "seq"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingRecord extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId; // Room 엔티티 전에는 FK 없이 Long으로 사용합니다.

    @Column(nullable = false)
    private Integer seq;

    @Column(name = "place_name", nullable = false)
    private String placeName;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @Column(name = "payer_room_member_id")
    private Long payerRoomMemberId;

    @Column(name = "payer_bank_name", length = 50)
    private String payerBankName;

    @Column(name = "payer_account_number", length = 50)
    private String payerAccountNumber;


    @Builder
    public MeetingRecord(Long roomId, Integer seq, String placeName, String memo, Long payerRoomMemberId, String payerBankName, String payerAccountNumber) {
        this.roomId = roomId;
        this.seq = seq;
        this.placeName = placeName;
        this.memo = memo;
        this.payerRoomMemberId = payerRoomMemberId;
        this.payerBankName = payerBankName;
        this.payerAccountNumber = payerAccountNumber;
    }

    public void update(String placeName, String memo, Long payerRoomMemberId, String payerBankName, String payerAccountNumber) {
        if (placeName != null) this.placeName = placeName;
        if (memo != null) this.memo = memo;
        if (payerRoomMemberId != null) this.payerRoomMemberId = payerRoomMemberId;
        if (payerBankName != null) this.payerBankName = payerBankName;
        if (payerAccountNumber != null) this.payerAccountNumber = payerAccountNumber;
    }
}
