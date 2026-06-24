package com.mog.project.meeting.service;

import com.mog.project.global.exception.ErrorCode;
import com.mog.project.global.exception.GlobalException;
import com.mog.project.meeting.dto.response.RoomPhotoResponse;
import com.mog.project.meeting.repository.RoomPhotoRepository;
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


    }
}
