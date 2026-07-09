package com.mog.project.domain.meeting.service;

import com.mog.project.global.config.S3Service;
import com.mog.project.global.exception.ErrorCode;
import com.mog.project.global.exception.GlobalException;
import com.mog.project.domain.meeting.dto.response.RoomPhotoResponse;
import com.mog.project.domain.meeting.entity.RoomPhoto;
import com.mog.project.domain.meeting.repository.RoomPhotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomPhotoService {
    private final RoomPhotoRepository roomPhotoRepository;

    private static final int MAX_PHOTO_COUNT = 3; // 3개
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10MB
    private static final List<String> ALLOWED_TYPES = List.of("image/jpeg", "image/png", "image/webp");
    private final S3Service s3Service;

    @Transactional
    public RoomPhotoResponse uploadPhoto(Long roomId, MultipartFile image) {

        // jpg, png, webp 외의 형식은 거부
        if (!ALLOWED_TYPES.contains(image.getContentType())) {
            throw new GlobalException(ErrorCode.INVALID_IMAGE);
        }

        // 10MB 초과 거부
        if (image.getSize() > MAX_FILE_SIZE) {
            throw new GlobalException(ErrorCode.IMAGE_TOO_LARGE);
        }

        // 이미 3장이면 거부
        if (roomPhotoRepository.countByRoomId(roomId) >= MAX_PHOTO_COUNT) {
            throw new GlobalException(ErrorCode.PHOTO_LIMIT_EXCEEDED);
        }

        // s3의 rooms/{roomId}/ 경로에 업로드를 진행
        String s3Url = s3Service.upload(image, "rooms/" + roomId);

        RoomPhoto photo = RoomPhoto.builder()
                .roomId(roomId)
                .s3Url(s3Url)
                .build();

        // 경로 주소를 저장
        roomPhotoRepository.save(photo);

        // 결과를 response DTO를 통해 반환
        return RoomPhotoResponse.from(photo);
    }

    // 사진 목록 조회 (주소를 반환)
    public List<RoomPhotoResponse> getPhotos(Long roomId) {
        return roomPhotoRepository.findByRoomId(roomId).stream()
                .map(RoomPhotoResponse::from)
                .toList();
    }

    @Transactional
    public void deletePhoto(Long roomId, Long photoId) {
        RoomPhoto photo = roomPhotoRepository.findByIdAndRoomId(photoId, roomId)
                .orElseThrow(() -> new GlobalException(ErrorCode.PHOTO_NOT_FOUND));

        s3Service.delete(photo.getS3Url());

        roomPhotoRepository.delete(photo);
    }


}
