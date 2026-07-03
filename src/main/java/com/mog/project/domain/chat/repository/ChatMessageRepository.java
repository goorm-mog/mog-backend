package com.mog.project.domain.chat.repository;

import com.mog.project.domain.chat.entity.ChatMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    // 히스토리 조회에서 씀 - 방안의 메시지들 시간순 정렬
    List<ChatMessage> findByRoomRoomIdOrderByCreatedAtAsc(Long roomId);

    
}
