package com.mog.project.domain.notification.controller;

import com.mog.project.domain.notification.dto.response.NotificationListResponse;
import com.mog.project.domain.notification.service.NotificationService;
import com.mog.project.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletResponse;
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
            description = "SSE 연결을 수립합니다. 연결 후 이벤트 발생 시 실시간으로 알림을 수신합니다.\n\n" +
                    "**인증 방식 주의**: 브라우저 `EventSource`는 `Authorization` 헤더를 지원하지 않습니다.\n\n" +
                    "- `?token={accessToken}` 쿼리 파라미터로 JWT를 전달해야 합니다.\n" +
                    "- 예시: `GET /api/v1/notifications/subscribe?token=eyJhbGci...`\n\n" +
                    "**이벤트 종류**\n" +
                    "- `connect`: 연결 직후 1회 전송\n" +
                    "- `notification`: 알림 발생 시 전송\n" +
                    "- `heartbeat`: 30초마다 연결 유지용 전송"
    )
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @Parameter(description = "JWT 액세스 토큰 (EventSource는 헤더 미지원으로 쿼리 파라미터로 전달)", required = true)
            @RequestParam String token,
            @AuthenticationPrincipal String kakaoId,
            HttpServletResponse response
    ) {
        response.setHeader("X-Accel-Buffering", "no");
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
