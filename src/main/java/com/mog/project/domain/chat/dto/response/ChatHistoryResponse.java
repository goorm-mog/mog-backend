package com.mog.project.domain.chat.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record ChatHistoryResponse (
    List<ChatMessageResponse> messages,
    LocalDateTime nextCursor,
    boolean hasNext
) {}