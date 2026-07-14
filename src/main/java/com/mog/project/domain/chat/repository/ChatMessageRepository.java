package com.mog.project.domain.chat.repository;

import com.mog.project.domain.chat.entity.ChatMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;              
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;       

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    // 히스토리 조회에서 씀 - 방안의 메시지들 시간순 정렬
    List<ChatMessage> findByRoomRoomIdOrderByCreatedAtAsc(Long roomId);

    // 첫 페이지 (cursor 없을 때) - 최신 50개
    List<ChatMessage> findTop50ByRoomRoomIdOrderByCreatedAtDesc(Long roomId);

    // 첫 페이지 (cursor 없을 때) - 최신 50개 -> Join으로 속도 개선
    @Query("SELECT m FROM ChatMessage m JOIN FETCH m.sender JOIN FETCH m.room WHERE m.room.roomId = :roomId ORDER BY m.createdAt DESC LIMIT :size")
    List<ChatMessage> findWithSenderByRoomId(@Param("roomId") Long roomId, @Param("size") int size);


    // 다음 페이지 (cursor 있을 때) - cursor 이전 시간의 50개
    List<ChatMessage> findTop50ByRoomRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc(Long roomId, LocalDateTime cursor);

    // 다음 페이지 (cursor 있을 때) - cursor 이전 시간의 50개 -> 속도 개선
    @Query("SELECT m FROM ChatMessage m JOIN FETCH m.sender JOIN FETCH m.room WHERE m.room.roomId = :roomId AND m.createdAt < :cursor ORDER BY m.createdAt DESC LIMIT :size")
    List<ChatMessage> findWithSenderByRoomIdAndCursor(@Param("roomId") Long roomId, @Param("cursor") LocalDateTime cursor, @Param("size") int size);

}
