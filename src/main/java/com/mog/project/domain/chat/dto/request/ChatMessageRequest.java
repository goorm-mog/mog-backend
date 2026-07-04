package com.mog.project.domain.chat.dto.request;

public record ChatMessageRequest(
    Long senderId,
    String senderName,
    String message
) {}