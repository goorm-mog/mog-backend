package com.mog.project.domain.chat.entity;

import com.mog.project.domain.room.entity.Room;
import com.mog.project.domain.user.entity.User;
import com.mog.project.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import jakarta.persistence.Index;
import lombok.*;


@Entity
@Table(name = "chat_messages", indexes = {
    @Index(name = "idx_chat_room_created", columnList
        = "room_id, created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long chatMessageId;

    @ManyToOne(fetch = FetchType.LAZY) // 이 메시지가 어느 방 소속인지
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY) // 이 메시지를 누가 보냈는지 (senderId, senderName 다 꺼냄)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(nullable = false, length = 1000) // 채팅 내용 1000자 제한
    private String message;

    @Builder
      public ChatMessage(Room room, User sender, String message) {
          this.room = room;
          this.sender = sender;
          this.message = message;
      }
}
