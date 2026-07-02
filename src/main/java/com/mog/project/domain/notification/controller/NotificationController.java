package com.mog.project.domain.notification.controller;

import com.mog.project.domain.notification.dto.response.NotificationListResponse;
import com.mog.project.domain.notification.service.NotificationService;
import com.mog.project.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "알림", description = "SSE 연결 / 알림 목록 조회 / 읽음 처리 / 전체 삭제 API")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(
            summary = "SSE 연결 수립",
            description = "SSE 연결을 수립합니다. 연결 후 이벤트 발생 시 실시간으로 알림을 수신합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal String kakaoId) {
        return notificationService.subscribe(kakaoId);
    }

    @Operation(
            summary = "알림 목록 조회",
            description = "최신순 최대 50건 반환. `unreadOnly=true` 전달 시 안읽은 알림만 반환합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping
    public ResponseEntity<ApiResponse<NotificationListResponse>> getNotifications(
            @AuthenticationPrincipal String kakaoId,
            @RequestParam(defaultValue = "false") boolean unreadOnly
    ) {
        return ResponseEntity.ok(
                ApiResponse.success("알림 목록을 조회했습니다.",
                        notificationService.getNotifications(kakaoId, unreadOnly))
        );
    }

    @Operation(
            summary = "단건 읽음 처리",
            description = "특정 알림을 읽음 상태로 변경합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @AuthenticationPrincipal String kakaoId,
            @PathVariable Long notificationId
    ) {
        notificationService.markAsRead(kakaoId, notificationId);
        return ResponseEntity.ok(ApiResponse.success("알림을 읽음 처리했습니다.", null));
    }

    @Operation(
            summary = "전체 알림 삭제",
            description = "유저의 모든 알림을 삭제합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteAll(@AuthenticationPrincipal String kakaoId) {
        notificationService.deleteAll(kakaoId);
        return ResponseEntity.ok(ApiResponse.success("모든 알림이 삭제되었습니다.", null));
    }

}
