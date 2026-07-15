package com.mog.project.domain.chat.controller;

import com.mog.project.domain.chat.dto.response.ChatHistoryResponse;
import com.mog.project.domain.chat.service.ChatService;
import com.mog.project.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;                               
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Chat", description = "채팅 API")
@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
public class ChatHistoryController {
    
    private final ChatService chatService;

    @Operation(
        summary = "채팅 히스토리 조회",
        description = "cursor 기반 페이지네이션. cursor 없으면 최신 50개, 있으면 그 이전 50개 반환.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/{roomId}/chat")
    public ResponseEntity<ApiResponse<ChatHistoryResponse>> getChatHistory(
        @AuthenticationPrincipal String kakaoId,
        @PathVariable Long roomId,
        @RequestParam(required = false) LocalDateTime cursor,
        @RequestParam(defaultValue = "50") int size
    ) {
        ChatHistoryResponse response = chatService.getHistory(kakaoId, roomId, cursor, size);
        return ResponseEntity.ok(
            ApiResponse.success("CHAT_HISTORY_FETCH_SUCCESS", "채팅 히스토리를 성공적으로 조회했습니다.", response)
        );
    }
}
