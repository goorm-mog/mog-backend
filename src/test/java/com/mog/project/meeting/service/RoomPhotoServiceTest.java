package com.mog.project.meeting.service;

import com.mog.project.domain.meeting.service.RoomPhotoService;
import com.mog.project.global.config.S3Service;
import com.mog.project.global.exception.GlobalException;
import com.mog.project.domain.meeting.dto.response.RoomPhotoResponse;
import com.mog.project.domain.meeting.entity.RoomPhoto;
import com.mog.project.domain.meeting.repository.RoomPhotoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomPhotoServiceTest {

    @Mock RoomPhotoRepository roomPhotoRepository;
    @Mock S3Service s3Service;
    @InjectMocks
    RoomPhotoService roomPhotoService;

    private MockMultipartFile validJpeg() {
        return new MockMultipartFile("image", "test.jpg", "image/jpeg", new byte[100]);
    }

    // ── uploadPhoto ─────────────────────────────────────────────────────────

    @Test
    void uploadPhoto_gif_형식이면_예외() {
        MockMultipartFile gif = new MockMultipartFile("image", "test.gif", "image/gif", new byte[100]);

        assertThatThrownBy(() -> roomPhotoService.uploadPhoto(1L, gif))
                .isInstanceOf(GlobalException.class);
    }

    @Test
    void uploadPhoto_10MB_초과면_예외() {
        byte[] bigFile = new byte[(int) (10L * 1024 * 1024 + 1)];
        MockMultipartFile large = new MockMultipartFile("image", "big.jpg", "image/jpeg", bigFile);

        assertThatThrownBy(() -> roomPhotoService.uploadPhoto(1L, large))
                .isInstanceOf(GlobalException.class);
    }

    @Test
    void uploadPhoto_이미_3장이면_예외() {
        when(roomPhotoRepository.countByRoomId(1L)).thenReturn(3);

        assertThatThrownBy(() -> roomPhotoService.uploadPhoto(1L, validJpeg()))
                .isInstanceOf(GlobalException.class);
    }

    @Test
    void uploadPhoto_정상_업로드시_S3URL_반환() {
        when(roomPhotoRepository.countByRoomId(1L)).thenReturn(0);
        when(s3Service.upload(any(), any())).thenReturn("https://s3.example.com/rooms/1/test.jpg");
        when(roomPhotoRepository.save(any())).thenAnswer(inv -> {
            RoomPhoto photo = inv.getArgument(0);
            ReflectionTestUtils.setField(photo, "id", 1L);
            return photo;
        });

        RoomPhotoResponse response = roomPhotoService.uploadPhoto(1L, validJpeg());

        assertThat(response.s3Url()).isEqualTo("https://s3.example.com/rooms/1/test.jpg");
        verify(s3Service).upload(any(), eq("rooms/1"));
        verify(roomPhotoRepository).save(any());
    }

    @Test
    void uploadPhoto_png와_webp도_허용() {
        when(roomPhotoRepository.countByRoomId(1L)).thenReturn(0);
        when(s3Service.upload(any(), any())).thenReturn("https://s3.example.com/photo.png");
        when(roomPhotoRepository.save(any())).thenAnswer(inv -> {
            RoomPhoto photo = inv.getArgument(0);
            ReflectionTestUtils.setField(photo, "id", 2L);
            return photo;
        });

        MockMultipartFile png = new MockMultipartFile("image", "test.png", "image/png", new byte[100]);

        assertThatCode(() -> roomPhotoService.uploadPhoto(1L, png)).doesNotThrowAnyException();
    }

    // ── deletePhoto ─────────────────────────────────────────────────────────

    @Test
    void deletePhoto_존재하지_않으면_예외() {
        when(roomPhotoRepository.findByIdAndRoomId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomPhotoService.deletePhoto(1L, 99L))
                .isInstanceOf(GlobalException.class);
    }

    @Test
    void deletePhoto_S3와_DB에서_모두_삭제() {
        RoomPhoto photo = RoomPhoto.builder()
                .roomId(1L).s3Url("https://s3.example.com/photo.jpg").build();
        when(roomPhotoRepository.findByIdAndRoomId(1L, 1L)).thenReturn(Optional.of(photo));

        roomPhotoService.deletePhoto(1L, 1L);

        verify(s3Service).delete("https://s3.example.com/photo.jpg");
        verify(roomPhotoRepository).delete(photo);
    }
}
