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
import com.mog.project.domain.room.dto.request.RoomStepRequest;
import com.mog.project.domain.room.dto.response.RoomCloseResponse;
import com.mog.project.domain.room.dto.response.RoomCreateResponse;
import com.mog.project.domain.room.dto.response.RoomListResponse;
import com.mog.project.domain.room.dto.response.RoomStatusResponse;
import com.mog.project.domain.room.dto.response.RoomStepResponse;
import com.mog.project.domain.room.entity.Room;
import com.mog.project.domain.room.entity.RoomMember;
import com.mog.project.domain.room.entity.RoomStatus;
import com.mog.project.domain.room.repository.RoomMemberRepository;
import com.mog.project.domain.room.repository.RoomRepository;
import com.mog.project.domain.meeting.repository.MeetingRecordRepository;
import com.mog.project.domain.user.entity.User;
import com.mog.project.domain.user.repository.UserRepository;
import com.mog.project.global.exception.AuthException;
import com.mog.project.global.exception.ErrorCode;
import com.mog.project.global.exception.GlobalException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock private RoomRepository roomRepository;
    @Mock private GroupRepository groupRepository;
    @Mock private GroupMemberRepository groupMemberRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoomMemberRepository roomMemberRepository;
    @Mock private MeetingRecordRepository meetingRecordRepository;
    @Mock private com.mog.project.domain.notification.service.NotificationService notificationService;

    private RoomService roomService;

    @BeforeEach
    void setUp() {
        roomService = new RoomService(roomRepository, groupRepository, groupMemberRepository, userRepository, roomMemberRepository, meetingRecordRepository, notificationService);
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

    // ---- 방 목록 조회 ----

    @Test
    void 그룹_멤버가_방_목록_조회에_성공한다() {
        User user = user("kakao-123");
        Group group = group("ABC123", "대학 친구들");
        GroupMember member = GroupMember.builder().group(group).user(user).role(GroupMemberRole.MEMBER).build();
        Room room1 = room(group, user, "강남역 삼겹살 모임", RoomStatus.VOTING);
        Room room2 = room(group, user, "홍대 술자리", RoomStatus.COMPLETED);

        given(userRepository.findByKakaoId("kakao-123")).willReturn(Optional.of(user));
        given(groupMemberRepository.findByGroupGroupIdAndUserUserId(1L, user.getUserId())).willReturn(Optional.of(member));
        given(roomRepository.findByGroupGroupIdAndDeletedAtIsNull(1L)).willReturn(List.of(room1, room2));

        RoomListResponse response = roomService.getRoomList("kakao-123", 1L);

        assertThat(response.rooms()).hasSize(2);
        assertThat(response.rooms().get(0).roomName()).isEqualTo("강남역 삼겹살 모임");
        assertThat(response.rooms().get(1).status()).isEqualTo(RoomStatus.COMPLETED);
    }

    @Test
    void 목록_조회시_인증되지_않은_유저면_UNAUTHORIZED_USER_예외가_발생한다() {
        given(userRepository.findByKakaoId("unknown")).willReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.getRoomList("unknown", 1L))
            .isInstanceOf(AuthException.class)
            .satisfies(e -> assertThat(((AuthException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED_USER));
    }

    @Test
    void 목록_조회시_그룹멤버가_아니면_NOT_GROUP_MEMBER_예외가_발생한다() {
        User user = user("kakao-123");

        given(userRepository.findByKakaoId("kakao-123")).willReturn(Optional.of(user));
        given(groupMemberRepository.findByGroupGroupIdAndUserUserId(1L, user.getUserId())).willReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.getRoomList("kakao-123", 1L))
            .isInstanceOf(GlobalException.class)
            .satisfies(e -> assertThat(((GlobalException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_GROUP_MEMBER));
    }

    // ---- 방 상태 및 멤버 현황 조회 ----

    @Test
    void 방_상태_및_멤버_현황_조회에_성공한다() {
        Group group = group("ABC123", "대학 친구들");
        User user1 = user("kakao-123");
        User user2 = user("kakao-456");
        Room room = room(group, user1, "강남역 삼겹살 모임", RoomStatus.RECORDING);
        RoomMember member1 = roomMember(room, user1, true);
        RoomMember member2 = roomMember(room, user2, false);

        given(roomRepository.findById(1L)).willReturn(Optional.of(room));
        given(meetingRecordRepository.findMaxSeqByRoomId(1L)).willReturn(2);
        given(roomMemberRepository.findByRoomRoomId(1L)).willReturn(List.of(member1, member2));

        RoomStatusResponse response = roomService.getRoomStatus(1L);

        assertThat(response.roomName()).isEqualTo("강남역 삼겹살 모임");
        assertThat(response.status()).isEqualTo(RoomStatus.RECORDING);
        assertThat(response.currentStep()).isEqualTo(2);
        assertThat(response.members()).hasSize(2);
        assertThat(response.members().get(0).isJoined()).isTrue();
        assertThat(response.members().get(1).isJoined()).isFalse();
    }

    @Test
    void 존재하지_않는_방이면_ROOM_NOT_FOUND_예외가_발생한다() {
        given(roomRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.getRoomStatus(999L))
            .isInstanceOf(GlobalException.class)
            .satisfies(e -> assertThat(((GlobalException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_NOT_FOUND));
    }

    // ---- 방 단계 진행 ----

    @Test
    void 방장이_방_단계를_변경하면_성공한다() {
        User leader = user("kakao-123");
        ReflectionTestUtils.setField(leader, "userId", 1L);
        Group group = group("ABC123", "대학 친구들");
        Room room = room(group, leader, "강남역 삼겹살 모임", RoomStatus.VOTING);

        given(userRepository.findByKakaoId("kakao-123")).willReturn(Optional.of(leader));
        given(roomRepository.findById(1L)).willReturn(Optional.of(room));
        given(roomRepository.saveAndFlush(room)).willReturn(room);

        RoomStepResponse response = roomService.updateRoomStep("kakao-123", 1L, new RoomStepRequest(RoomStatus.RECORDING));

        assertThat(response.currentStatus()).isEqualTo(RoomStatus.RECORDING);
        assertThat(room.getStatus()).isEqualTo(RoomStatus.RECORDING);
    }

    @Test
    void 방장이_아니면_NOT_ROOM_LEADER_예외가_발생한다() {
        User leader = user("kakao-123");
        ReflectionTestUtils.setField(leader, "userId", 1L);
        User other = user("kakao-456");
        ReflectionTestUtils.setField(other, "userId", 2L);
        Group group = group("ABC123", "대학 친구들");
        Room room = room(group, leader, "강남역 삼겹살 모임", RoomStatus.VOTING);

        given(userRepository.findByKakaoId("kakao-456")).willReturn(Optional.of(other));
        given(roomRepository.findById(1L)).willReturn(Optional.of(room));

        assertThatThrownBy(() -> roomService.updateRoomStep("kakao-456", 1L, new RoomStepRequest(RoomStatus.RECORDING)))
            .isInstanceOf(GlobalException.class)
            .satisfies(e -> assertThat(((GlobalException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_ROOM_LEADER));
    }

    @Test
    void 단계_변경시_존재하지_않는_방이면_ROOM_NOT_FOUND_예외가_발생한다() {
        given(userRepository.findByKakaoId("kakao-123")).willReturn(Optional.of(user("kakao-123")));
        given(roomRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.updateRoomStep("kakao-123", 999L, new RoomStepRequest(RoomStatus.RECORDING)))
            .isInstanceOf(GlobalException.class)
            .satisfies(e -> assertThat(((GlobalException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_NOT_FOUND));
    }

    @Test
    void 단계_변경시_인증되지_않은_유저면_UNAUTHORIZED_USER_예외가_발생한다() {
        given(userRepository.findByKakaoId("unknown")).willReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.updateRoomStep("unknown", 1L, new RoomStepRequest(RoomStatus.RECORDING)))
            .isInstanceOf(AuthException.class)
            .satisfies(e -> assertThat(((AuthException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED_USER));
    }

    // ---- 방 종료 ----

    @Test
    void 방장이_방을_종료하면_소프트_삭제된다() {
        User leader = user("kakao-123");
        ReflectionTestUtils.setField(leader, "userId", 1L);
        Group group = group("ABC123", "대학 친구들");
        Room room = room(group, leader, "강남역 삼겹살 모임", RoomStatus.RECORDING);

        given(userRepository.findByKakaoId("kakao-123")).willReturn(Optional.of(leader));
        given(roomRepository.findById(1L)).willReturn(Optional.of(room));

        RoomCloseResponse response = roomService.closeRoom("kakao-123", 1L);

        assertThat(response.status()).isEqualTo(RoomStatus.COMPLETED);
        assertThat(response.deletedAt()).isNotNull();
        assertThat(room.getStatus()).isEqualTo(RoomStatus.COMPLETED);
        assertThat(room.getDeletedAt()).isNotNull();
    }

    @Test
    void 방장이_아니면_방_종료시_NOT_ROOM_LEADER_예외가_발생한다() {
        User leader = user("kakao-123");
        ReflectionTestUtils.setField(leader, "userId", 1L);
        User other = user("kakao-456");
        ReflectionTestUtils.setField(other, "userId", 2L);
        Group group = group("ABC123", "대학 친구들");
        Room room = room(group, leader, "강남역 삼겹살 모임", RoomStatus.RECORDING);

        given(userRepository.findByKakaoId("kakao-456")).willReturn(Optional.of(other));
        given(roomRepository.findById(1L)).willReturn(Optional.of(room));

        assertThatThrownBy(() -> roomService.closeRoom("kakao-456", 1L))
            .isInstanceOf(GlobalException.class)
            .satisfies(e -> assertThat(((GlobalException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_ROOM_LEADER));
    }

    @Test
    void 방_종료시_존재하지_않는_방이면_ROOM_NOT_FOUND_예외가_발생한다() {
        given(userRepository.findByKakaoId("kakao-123")).willReturn(Optional.of(user("kakao-123")));
        given(roomRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.closeRoom("kakao-123", 999L))
            .isInstanceOf(GlobalException.class)
            .satisfies(e -> assertThat(((GlobalException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROOM_NOT_FOUND));
    }

    @Test
    void 방_종료시_인증되지_않은_유저면_UNAUTHORIZED_USER_예외가_발생한다() {
        given(userRepository.findByKakaoId("unknown")).willReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.closeRoom("unknown", 1L))
            .isInstanceOf(AuthException.class)
            .satisfies(e -> assertThat(((AuthException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED_USER));
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

    private Room room(Group group, User creator, String roomName, RoomStatus status) {
        return Room.builder()
            .group(group)
            .creator(creator)
            .roomName(roomName)
            .status(status)
            .build();
    }

    private RoomMember roomMember(Room room, User user, boolean isJoined) {
        return RoomMember.builder()
            .room(room)
            .user(user)
            .isJoined(isJoined)
            .build();
    }
}
