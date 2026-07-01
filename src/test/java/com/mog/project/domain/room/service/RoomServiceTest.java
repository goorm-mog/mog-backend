package com.mog.project.domain.room.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.mog.project.domain.groups.entity.Group;
import com.mog.project.domain.groups.entity.GroupMember;
import com.mog.project.domain.groups.entity.GroupMemberRole;
import com.mog.project.domain.groups.repository.GroupMemberRepository;
import com.mog.project.domain.groups.repository.GroupRepository;
import com.mog.project.domain.room.dto.request.RoomCreateRequest;
import com.mog.project.domain.room.dto.response.RoomCreateResponse;
import com.mog.project.domain.room.entity.Room;
import com.mog.project.domain.room.entity.RoomStatus;
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

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock private RoomRepository roomRepository;
    @Mock private GroupRepository groupRepository;
    @Mock private GroupMemberRepository groupMemberRepository;
    @Mock private UserRepository userRepository;

    private RoomService roomService;

    @BeforeEach
    void setUp() {
        roomService = new RoomService(roomRepository, groupRepository, groupMemberRepository, userRepository);
    }

    // ---- 방 생성 ----

    @Test
    void 그룹_멤버가_방_생성에_성공한다() {
        User user = user("kakao-123");
        Group group = group("ABC123", "대학 친구들");
        GroupMember member = GroupMember.builder().group(group).user(user).role(GroupMemberRole.MEMBER).build();

        given(userRepository.findByKakaoId("kakao-123")).willReturn(Optional.of(user));
        given(groupRepository.findById(1L)).willReturn(Optional.of(group));
        given(groupMemberRepository.findByGroupGroupIdAndUserUserId(1L, user.getUserId())).willReturn(Optional.of(member));

        RoomCreateResponse response = roomService.createRoom("kakao-123", 1L, new RoomCreateRequest("강남역 삼겹살 모임"));

        assertThat(response.roomName()).isEqualTo("강남역 삼겹살 모임");
        assertThat(response.status()).isEqualTo(RoomStatus.VOTING);
        verify(roomRepository).save(any(Room.class));
    }

    @Test
    void 인증되지_않은_유저면_UNAUTHORIZED_USER_예외가_발생한다() {
        given(userRepository.findByKakaoId("unknown")).willReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.createRoom("unknown", 1L, new RoomCreateRequest("강남역 삼겹살 모임")))
            .isInstanceOf(AuthException.class)
            .satisfies(e -> assertThat(((AuthException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED_USER));
    }

    @Test
    void 존재하지_않는_그룹이면_GROUP_NOT_FOUND_예외가_발생한다() {
        given(userRepository.findByKakaoId("kakao-123")).willReturn(Optional.of(user("kakao-123")));
        given(groupRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.createRoom("kakao-123", 999L, new RoomCreateRequest("강남역 삼겹살 모임")))
            .isInstanceOf(GlobalException.class)
            .satisfies(e -> assertThat(((GlobalException) e).getErrorCode())
                .isEqualTo(ErrorCode.GROUP_NOT_FOUND));
    }

    @Test
    void 그룹멤버가_아니면_NOT_GROUP_MEMBER_예외가_발생한다() {
        User user = user("kakao-123");
        Group group = group("ABC123", "대학 친구들");

        given(userRepository.findByKakaoId("kakao-123")).willReturn(Optional.of(user));
        given(groupRepository.findById(1L)).willReturn(Optional.of(group));
        given(groupMemberRepository.findByGroupGroupIdAndUserUserId(1L, user.getUserId())).willReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.createRoom("kakao-123", 1L, new RoomCreateRequest("강남역 삼겹살 모임")))
            .isInstanceOf(GlobalException.class)
            .satisfies(e -> assertThat(((GlobalException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_GROUP_MEMBER));
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

    private Group group(String inviteCode, String groupName) {
        return Group.builder()
            .groupName(groupName)
            .inviteCode(inviteCode)
            .kakaoShareUrl("https://mo-ge.site/join?code=" + inviteCode)
            .build();
    }
}
