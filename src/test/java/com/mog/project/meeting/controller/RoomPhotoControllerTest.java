package com.mog.project.meeting.controller;

import com.mog.project.meeting.dto.response.RoomPhotoResponse;
import com.mog.project.meeting.service.RoomPhotoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoomPhotoController.class)
@WithMockUser
@ActiveProfiles("test")
class RoomPhotoControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean RoomPhotoService roomPhotoService;

    // ── POST /api/v1/rooms/{roomId}/photos ──────────────────────────────────

    @Test
    void uploadPhoto_201_반환() throws Exception {
        RoomPhotoResponse response = new RoomPhotoResponse(
                1L, "https://s3.example.com/rooms/1/photo.jpg", LocalDateTime.now()
        );
        when(roomPhotoService.uploadPhoto(eq(1L), any())).thenReturn(response);

        MockMultipartFile image = new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", new byte[100]
        );

        mockMvc.perform(multipart("/api/v1/rooms/1/photos")
                        .file(image)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.photoId").value(1))
                .andExpect(jsonPath("$.data.s3Url").value("https://s3.example.com/rooms/1/photo.jpg"));
    }

    // ── DELETE /api/v1/rooms/{roomId}/photos/{photoId} ──────────────────────

    @Test
    void deletePhoto_200_반환() throws Exception {
        doNothing().when(roomPhotoService).deletePhoto(1L, 5L);

        mockMvc.perform(delete("/api/v1/rooms/1/photos/5")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("사진을 삭제했습니다."));
    }
}
