package com.mog.project.domain.chat.service;

import com.mog.project.domain.chat.dto.request.ChatMessageRequest;
import com.mog.project.domain.chat.dto.response.ChatMessageResponse;
import com.mog.project.domain.chat.entity.ChatMessage;
import com.mog.project.domain.chat.repository.ChatMessageRepository;
import   com.mog.project.domain.chat.dto.response.ChatHistoryResponse;
import com.mog.project.domain.room.entity.Room;
import com.mog.project.domain.room.repository.RoomMemberRepository;
import com.mog.project.domain.room.repository.RoomRepository;
import com.mog.project.domain.user.entity.User;
import com.mog.project.domain.user.repository.UserRepository;
import com.mog.project.global.exception.AuthException;
import com.mog.project.global.exception.ErrorCode;
import com.mog.project.global.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public ChatMessageResponse saveMessage(String kakaoId, Long roomId, ChatMessageRequest request) {
        // 1. Principal로 실제 유저 조회
        // 요청 바디의 senderId/senderName은 여기서 안씀 (스푸닝 방지)
        User sender = userRepository.findByKakaoId(kakaoId)
            .orElseThrow(() -> new AuthException(ErrorCode.UNAUTHORIZED_USER));

        // 2. 이 유저가 진짜 이 방 멤버인지 검증
        if (!roomMemberRepository.existsByRoomRoomIdAndUserUserId(roomId, sender.getUserId())) {
            throw new GlobalException(ErrorCode.NOT_ROOM_MEMBER);
        }

        // 3. 방이 실제 존재하고 삭제 안 됐는지 확인
        Room room = roomRepository.findById(roomId)
            .filter(r -> r.getDeletedAt() == null)
            .orElseThrow(() -> new GlobalException(ErrorCode.ROOM_NOT_FOUND));

        // 4. 메시지 엔티티 만들어서 DB 저장
        ChatMessage chatMessage = ChatMessage.builder()
            .room(room)
            .sender(sender)
            .message(request.message())
            .build();
        chatMessageRepository.save(chatMessage);

        // 5. 저장 된 엔티티 -> 응답 DTO 변환해서 리턴
        return ChatMessageResponse.from(chatMessage);
    }

    @Transactional(readOnly = true)
    public ChatHistoryResponse getHistory(String kakaoId, Long roomId, LocalDateTime cursor, int size) {
        User user = userRepository.findByKakaoId(kakaoId)
            .orElseThrow(() -> new AuthException(ErrorCode.UNAUTHORIZED_USER));

        if (!roomMemberRepository.existsByRoomRoomIdAndUserUserId(roomId, user.getUserId())) {
            throw new GlobalException(ErrorCode.NOT_ROOM_MEMBER);
        }

        List<ChatMessage> messages = (cursor == null)
            ? chatMessageRepository.findWithSenderByRoomId(roomId, size)
            : chatMessageRepository.findWithSenderByRoomIdAndCursor(roomId, cursor, size);

        boolean hasNext = messages.size() == size;
        LocalDateTime nextCursor = hasNext ? messages.get(messages.size() - 1).getCreatedAt() : null;

        List<ChatMessageResponse> result = messages.reversed()
            .stream()
            .map(ChatMessageResponse::from)
            .toList();

        return new ChatHistoryResponse(result, nextCursor, hasNext);
    }
}
