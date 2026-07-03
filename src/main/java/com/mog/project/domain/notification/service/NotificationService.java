package com.mog.project.domain.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mog.project.domain.notification.dto.response.NotificationListResponse;
import com.mog.project.domain.notification.dto.response.NotificationResponse;
import com.mog.project.domain.notification.entity.Notification;
import com.mog.project.domain.notification.entity.NotificationType;
import com.mog.project.domain.notification.repository.NotificationRepository;
import com.mog.project.domain.notification.sse.SseEmitterManager;
import com.mog.project.domain.user.entity.User;
import com.mog.project.domain.user.repository.UserRepository;
import com.mog.project.global.exception.ErrorCode;
import com.mog.project.global.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SseEmitterManager sseEmitterManager;
    private final ObjectMapper objectMapper;

    // SSE 연결 수립: 유저 확인 후 emitter를 반환
    public SseEmitter subscribe(String kakaoId) {
        User user = getUser(kakaoId);
        return sseEmitterManager.subscribe(user.getUserId());
    }


    // 알림 목록 조회
    @Transactional(readOnly = true)
    public NotificationListResponse getNotifications(String kakaoId, boolean unreadOnly)  {
        User user = getUser(kakaoId);
        Long userId = user.getUserId();
        List<NotificationResponse> notification = unreadOnly ? notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .stream().map(NotificationResponse::from).toList()
                : notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(NotificationResponse::from).toList();

        int unreadCount = (int) notificationRepository.countByUserIdAndIsReadFalse(userId);

        return new NotificationListResponse(unreadCount, notification);
    }

    @Transactional
    public void markAsRead(String kakaoId, Long notificationId) {
        User user = getUser(kakaoId);

        Notification notification = notificationRepository.findByIdAndUserId(notificationId, user.getUserId())
                .orElseThrow(() -> new GlobalException(ErrorCode.NOTIFICATION_NOT_FOUND));

        notification.markAsRead();
    }

    // 전체 알림 삭제
    @Transactional
    public void deleteAll(String kakaoId) {
        User user = getUser(kakaoId);
        notificationRepository.deleteAllByUserId(user.getUserId());
    }

    // 알림 생성 및 DB 저장, Redis publish
    @Transactional
    public void send(Long userId, NotificationType type, String message, Long roomId) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .message(message)
                .roomId(roomId)
                .build();

        notificationRepository.save(notification);

        try {
            String payload = objectMapper.writeValueAsString(NotificationResponse.from(notification));
            sseEmitterManager.publish(userId, payload);
        } catch (JsonProcessingException ignored) {
            // publish 실패해도 DB는 저장합니다.
        }
    }

    // 유저 검색
    private User getUser(String kakaoId) {
        return userRepository.findByKakaoId(kakaoId)
                .orElseThrow(() -> new GlobalException(ErrorCode.FORBIDDEN));
    }

}
