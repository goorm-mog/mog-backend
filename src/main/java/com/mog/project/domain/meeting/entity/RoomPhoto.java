package com.mog.project.domain.meeting.entity;

import com.mog.project.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "room_photo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomPhoto extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "s3_url", nullable = false)
    private String s3Url;

    @Builder
    public RoomPhoto(Long roomId, String s3Url) {
        this.roomId = roomId;
        this.s3Url = s3Url;
    }
}
