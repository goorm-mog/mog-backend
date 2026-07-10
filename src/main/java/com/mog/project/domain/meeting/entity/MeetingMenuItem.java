package com.mog.project.domain.meeting.entity;

import com.mog.project.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "meeting_menu_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MeetingMenuItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_record_id", nullable = false)
    private MeetingRecord meetingRecord;

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Integer price;

    @Builder
    public MeetingMenuItem(MeetingRecord meetingRecord, String itemName, Integer quantity, Integer price) {
        this.meetingRecord = meetingRecord;
        this.itemName = itemName;
        this.quantity = quantity;
        this.price = price;
    }

}
