package com.mog.project.meeting.entity;

import com.mog.project.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "meeting_member_cost",
        uniqueConstraints = @UniqueConstraint(columnNames = {"meeting_record_id", "room_member_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingMemberCost extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_record_id", nullable = false)
    private MeetingRecord meetingRecord;

    @Column(name = "room_member_id", nullable = false)
    private Long roomMemberId;

    @Column(nullable = false)
    private Integer amount = 0;

    @Builder
    public MeetingMemberCost(MeetingRecord meetingRecord, Long roomMemberId, Integer amount) {
        this.meetingRecord = meetingRecord;
        this.roomMemberId = roomMemberId;
        this.amount = amount;
    }

}
