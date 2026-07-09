package com.mog.project.domain.notification.controller;

import com.mog.project.domain.notification.dto.response.NotificationListResponse;
import com.mog.project.domain.notification.dto.response.NotificationResponse;
import com.mog.project.domain.notification.entity.NotificationType;
import com.mog.project.domain.notification.service.NotificationService;
import com.mog.project.global.exception.ErrorCode;
import com.mog.project.global.exception.GlobalException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@WithMockUser
@ActiveProfiles("test")
class NotificationControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean NotificationService notificationService;

    private NotificationResponse sampleNotification() {
        return new NotificationResponse(
                1L,
                NotificationType.SETTLEMENT_DONE,
                "[친구들] 정산이 완료됐습니다.",
                5L,
                false,
                LocalDateTime.now()
        );
    }

    // ── GET /api/v1/notifications/subscribe ────────────────────────────────

    @Test
    void subscribe_성공시_SSE_연결_수립() throws Exception {
        when(notificationService.subscribe(any())).thenReturn(new SseEmitter());

        mockMvc.perform(get("/api/v1/notifications/subscribe")
                        .param("token", "test-token")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk());
    }

    @Test
    void subscribe_token_없으면_400() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/subscribe")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/v1/notifications ───────────────────────────────────────────

    @Test
    void getNotifications_성공시_200_반환() throws Exception {
        NotificationListResponse response = new NotificationListResponse(1, List.of(sampleNotification()));
        when(notificationService.getNotifications(any(), eq(false))).thenReturn(response);

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("알림 목록을 조회했습니다."))
                .andExpect(jsonPath("$.data.unreadCount").value(1))
                .andExpect(jsonPath("$.data.notifications").isArray())
                .andExpect(jsonPath("$.data.notifications[0].type").value("SETTLEMENT_DONE"));
    }

    @Test
    void getNotifications_unreadOnly_true_전달시_미읽음만_반환() throws Exception {
        NotificationListResponse response = new NotificationListResponse(1, List.of(sampleNotification()));
        when(notificationService.getNotifications(any(), eq(true))).thenReturn(response);

        mockMvc.perform(get("/api/v1/notifications")
                        .param("unreadOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(1));
    }

    @Test
    void getNotifications_빈_목록이면_200_반환() throws Exception {
        NotificationListResponse response = new NotificationListResponse(0, List.of());
        when(notificationService.getNotifications(any(), eq(false))).thenReturn(response);

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(0))
                .andExpect(jsonPath("$.data.notifications").isEmpty());
    }

    // ── PATCH /api/v1/notifications/{notificationId}/read ──────────────────

    @Test
    void markAsRead_성공시_200_반환() throws Exception {
        doNothing().when(notificationService).markAsRead(any(), eq(1L));

        mockMvc.perform(patch("/api/v1/notifications/1/read")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("알림을 읽음 처리했습니다."));
    }

    @Test
    void markAsRead_알림_없으면_404() throws Exception {
        doThrow(new GlobalException(ErrorCode.NOTIFICATION_NOT_FOUND))
                .when(notificationService).markAsRead(any(), eq(999L));

        mockMvc.perform(patch("/api/v1/notifications/999/read")
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void markAsRead_다른_유저_알림이면_403() throws Exception {
        doThrow(new GlobalException(ErrorCode.FORBIDDEN))
                .when(notificationService).markAsRead(any(), eq(1L));

        mockMvc.perform(patch("/api/v1/notifications/1/read")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ── DELETE /api/v1/notifications ───────────────────────────────────────

    @Test
    void deleteAll_성공시_200_반환() throws Exception {
        doNothing().when(notificationService).deleteAll(any());

        mockMvc.perform(delete("/api/v1/notifications")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("모든 알림이 삭제되었습니다."));
    }
}
