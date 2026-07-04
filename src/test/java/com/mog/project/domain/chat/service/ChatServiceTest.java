package com.mog.project.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.mog.project.domain.chat.dto.request.ChatMessageRequest;
import com.mog.project.domain.chat.dto.response.ChatMessageResponse;
import com.mog.project.domain.chat.entity.ChatMessage;
import com.mog.project.domain.chat.repository.ChatMessageRepository;
import com.mog.project.domain.groups.entity.Group;
import com.mog.project.domain.room.entity.Room;
import com.mog.project.domain.room.entity.RoomStatus;
import com.mog.project.domain.room.repository.RoomMemberRepository;
import com.mog.project.domain.room.repository.RoomRepository;
import com.mog.project.domain.user.entity.User;
import com.mog.project.domain.user.repository.UserRepository;
import com.mog.project.global.exception.AuthException;
import com.mog.project.global.exception.ErrorCode;
import com.mog.project.global.exception.GlobalException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private RoomMemberRepository roomMemberRepository;
    @Mock private UserRepository userRepository;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(chatMessageRepository, roomRepository, roomMemberRepository, userRepository);
    }

    @Test
    void 방_멤버가_채팅을_보내면_저장하고_응답을_반환한다() {
        User sender = user("kakao-123");
        ReflectionTestUtils.setField(sender, "userId", 1L);
        Room room = room(RoomStatus.RECORDING);
        ReflectionTestUtils.setField(room, "roomId", 1L);

        given(userRepository.findByKakaoId("kakao-123")).willReturn(Optional.of(sender));
        given(roomMemberRepository.existsByRoomRoomIdAndUserUserId(1L, 1L)).willReturn(true);
        given(roomRepository.findById(1L)).willReturn(Optional.of(room));
        given(chatMessageRepository.save(any(ChatMessage.class)))
            .willAnswer(inv -> inv.getArgument(0));

        ChatMessageResponse response = chatService.saveMessage(
            "kakao-123", 1L, new ChatMessageRequest(1L, "조성민", "강남역 몇 번 출구에서 만나?")
        );

        assertThat(response.roomId()).isEqualTo(1L);
        assertThat(response.senderId()).isEqualTo(1L);
        assertThat(response.senderName()).isEqualTo("테스트유저");
        assertThat(response.message()).isEqualTo("강남역 몇 번 출구에서 만나?");
    }

    @Test
    void 인증되지_않은_유저면_UNAUTHORIZED_USER_예외가_발생한다() {
        given(userRepository.findByKakaoId("unknown")).willReturn(Optional.empty());

        assertThatThrownBy(() ->
            chatService.saveMessage("unknown", 1L, new ChatMessageRequest(1L, "이름", "메시지"))
        )
            .isInstanceOf(AuthException.class)
            .satisfies(e -> assertThat(((AuthException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED_USER));
    }

    @Test
    void 방_멤버가_아니면_NOT_ROOM_MEMBER_예외가_발생한다() {
        User sender = user("kakao-123");
        ReflectionTestUtils.setField(sender, "userId", 1L);

        given(userRepository.findByKakaoId("kakao-123")).willReturn(Optional.of(sender));
        given(roomMemberRepository.existsByRoomRoomIdAndUserUserId(1L, 1L)).willReturn(false);

        assertThatThrownBy(() ->
            chatService.saveMessage("kakao-123", 1L, new ChatMessageRequest(1L, "이름", "메시지"))
        )
            .isInstanceOf(GlobalException.class)
            .satisfies(e -> assertThat(((GlobalException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_ROOM_MEMBER));
    }

    @Test
    void 존재하지_않는_방이면_ROOM_NOT_FOUND_예외가_발생한다() {
        User sender = user("kakao-123");
        ReflectionTestUtils.setField(sender, "userId", 1L);

        given(userRepository.findByKakaoId("kakao-123")).willReturn(Optional.of(sender));
        given(roomMemberRepository.existsByRoomRoomIdAndUserUserId(999L, 1L)).willReturn(true);
        given(roomRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() ->
            chatService.saveMessage("kakao-123", 999L, new ChatMessageRequest(1L, "이름", "메시지"))
        )
            .isInstanceOf(GlobalException.class)
            .satisfies(e -> assertThat(((GlobalException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_NOT_FOUND));
    }

    @Test
    void 삭제된_방이면_ROOM_NOT_FOUND_예외가_발생한다() {
        User sender = user("kakao-123");
        ReflectionTestUtils.setField(sender, "userId", 1L);
        Room deletedRoom = room(RoomStatus.RECORDING);
        ReflectionTestUtils.setField(deletedRoom, "roomId", 1L);
        deletedRoom.close();

        given(userRepository.findByKakaoId("kakao-123")).willReturn(Optional.of(sender));
        given(roomMemberRepository.existsByRoomRoomIdAndUserUserId(1L, 1L)).willReturn(true);
        given(roomRepository.findById(1L)).willReturn(Optional.of(deletedRoom));

        assertThatThrownBy(() ->
            chatService.saveMessage("kakao-123", 1L, new ChatMessageRequest(1L, "이름", "메시지"))
        )
            .isInstanceOf(GlobalException.class)
            .satisfies(e -> assertThat(((GlobalException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_NOT_FOUND));
    }

    // ---- 헬퍼 ----

    private User user(String kakaoId) {
        return User.builder()
            .kakaoId(kakaoId)
            .nickname("테스트유저")
            .email("test@kakao.com")
            .profileImageUrl(null)
            .build();
    }

    private Room room(RoomStatus status) {
        Group group = Group.builder()
            .groupName("대학 친구들")
            .inviteCode("ABC123")
            .kakaoShareUrl("https://mo-ge.site/join?code=ABC123")
            .build();
        return Room.builder()
            .group(group)
            .creator(user("kakao-creator"))
            .roomName("강남역 삼겹살 모임")
            .status(status)
            .build();
    }
}
