package com.mog.project.domain.meeting.dto.response;

import com.mog.project.domain.meeting.entity.RoomPhoto;

import java.time.LocalDateTime;

// 사진 반환
public record RoomPhotoResponse(

        // 사진 ID
        Long photoId,

        // S3에 저장된 사진 URL
        String s3Url,

        // 업로드 일시
        LocalDateTime createdAt

) {
    public static RoomPhotoResponse from(RoomPhoto photo) {
        return new RoomPhotoResponse(
                photo.getId(),
                photo.getS3Url(),
                photo.getCreatedAt()
        );
    }
}
