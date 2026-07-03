package com.mog.project.domain.chat.controller;

import com.mog.project.domain.chat.dto.response.ChatMessageResponse;
import com.mog.project.domain.chat.service.ChatService;
import com.mog.project.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Chat", description = "채팅 API")
@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
public class ChatHistoryController {
    
    private final ChatService chatService;

    @Operation(
        summary = "채팅 히스토리 조회",
        description = "방에 실시간으로 접속해 있지 않아도 방 멤버라면 지금까지 쌓인 채팅 기록을 조회합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/{roomId}/chat")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getChatHistory(
        @AuthenticationPrincipal String kakaoId,
        @PathVariable Long roomId
    ) {
        List<ChatMessageResponse> response = chatService.getHistory(kakaoId, roomId);
        return ResponseEntity.ok(
            ApiResponse.success("CHAT_HISTORY_FETCH_SUCCESS", "채팅 히스토리를 성공적으로 조회했습니다.", response)
        );
    }
}
