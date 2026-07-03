package com.mog.project.domain.chat.controller;

import com.mog.project.domain.chat.dto.request.ChatMessageRequest;
import com.mog.project.domain.chat.dto.response.ChatMessageResponse;
import com.mog.project.domain.chat.service.ChatService;
import com.mog.project.global.exception.AuthException;
import com.mog.project.global.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller // @RestController x -> 리턴값을 HTTP 응답으로 안 쓰고 STOMP 메시지 핸들러
@RequiredArgsConstructor
public class ChatController {
    
    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate; // 구독자들한테 쏘는 도구

    // "/pub" + 아래 경로 = 클라이언트가 실제 보내는 주소 : /pub/api/v1/rooms/{roomId}/chat
    @MessageMapping("/api/v1/rooms/{roomId}/chat")
    public void sendMessage(
        @DestinationVariable Long roomId,     // 주소의 {roomId} 부분 그대로 꺼냄
        @Payload ChatMessageRequest request,  // 클라이언트가 보낸 JSON body
        Principal principal                   // setUser()로 심어둔 신원(kakaoId)
    ) {
        try {
            ChatMessageResponse response = chatService.saveMessage(principal.getName(), roomId, request);

            // "/sub" + roomId로 브로드 캐스트 -> 그 주소 구독중인 모든 클라이언트한테 감
            messagingTemplate.convertAndSend("/sub/api/v1/rooms/" + roomId, response);
        } catch (GlobalException e) {
            // 검증 실패 시 여기서 걸러짐 - 로그만 남기고 broadcast 안함
            log.warn("[ChatController] 채팅 발행 실패: {}", e.getMessage());
        }
    }
}
