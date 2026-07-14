package com.mog.project.domain.chat.dto.response;

import java.time.LocalDateTime;

public record ChatMessageResponse (
    Long roomId,
    Long senderId,
    String senderKakaoId,
    String senderName,
    String message,
    LocalDateTime timestamp
) {
    // ChatMessage 엔티티 -> 응답 DTO 변환
    public static ChatMessageResponse from(com.mog.project.domain.chat.entity.ChatMessage chatMessage) {
        return new ChatMessageResponse(
            chatMessage.getRoom().getRoomId(),
            chatMessage.getSender().getUserId(),
            chatMessage.getSender().getKakaoId(),
            chatMessage.getSender().getNickname(),
            chatMessage.getMessage(),
            chatMessage.getCreatedAt()
        );
    }
}
