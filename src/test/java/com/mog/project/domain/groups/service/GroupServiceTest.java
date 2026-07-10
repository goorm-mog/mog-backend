package com.mog.project.domain.groups.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.mog.project.domain.groups.dto.request.GroupJoinRequest;
import com.mog.project.domain.groups.dto.request.GroupUpdateRequest;
import com.mog.project.domain.groups.dto.response.GroupDeleteResponse;
import com.mog.project.domain.groups.dto.response.GroupDetailResponse;
import com.mog.project.domain.groups.dto.response.GroupJoinResponse;
import com.mog.project.domain.groups.dto.response.GroupLeaveResponse;
import com.mog.project.domain.groups.dto.response.GroupListResponse;
import com.mog.project.domain.groups.dto.response.GroupUpdateResponse;
import com.mog.project.domain.groups.entity.Group;
import com.mog.project.domain.groups.entity.GroupMember;
import com.mog.project.domain.groups.entity.GroupMemberRole;
import com.mog.project.domain.groups.repository.GroupMemberRepository;
import com.mog.project.domain.groups.repository.GroupRepository;
import com.mog.project.domain.room.entity.Room;
import com.mog.project.domain.room.entity.RoomStatus;
import com.mog.project.domain.room.repository.RoomRepository;
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

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock private GroupRepository groupRepository;
    @Mock private GroupMemberRepository groupMemberRepository;
    @Mock private UserRepository userRepository;
    @Mock private RoomRepository roomRepository;

    private GroupService groupService;

    @BeforeEach
    void setUp() {
        groupService = new GroupService(groupRepository, groupMemberRepository, userRepository, roomRepository);
    }

    // ---- 그룹 참여 ----

    @Test
    void 유효한_초대코드로_그룹_참여에_성공한다() {
        User user = user("kakao-123");
        Group group = group("ABC123", "테스트그룹");
        given(userRepository.findByKakaoId("kakao-123")).willReturn(Optional.of(user));
        given(groupRepository.findByInviteCode("ABC123")).willReturn(Optional.of(group));
        given(groupMemberRepository.existsByGroupGroupIdAndUserUserId(any(), any())).willReturn(false);

        GroupJoinResponse response = groupService.joinGroup("kakao-123", new GroupJoinRequest("ABC123"));

        assertThat(response.groupName()).isEqualTo("테스트그룹");
        assertThat(response.role()).isEqualTo(GroupMemberRole.MEMBER);
        verify(groupMemberRepository).save(any(GroupMember.class));
    }

    @Test
    void 존재하지_않는_초대코드면_GROUP_NOT_FOUND_예외가_발생한다() {
        given(userRepository.findByKakaoId("kakao-123")).willReturn(Optional.of(user("kakao-123")));
        given(groupRepository.findByInviteCode("XXXXXX")).willReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.joinGroup("kakao-123", new GroupJoinRequest("XXXXXX")))
            .isInstanceOf(GlobalException.class)
            .satisfies(e -> assertThat(((GlobalException) e).getErrorCode())
                .isEqualTo(ErrorCode.GROUP_NOT_FOUND));
    }

    @Test
    void 이미_가입된_멤버가_재가입하면_ALREADY_JOINED_MEMBER_예외가_발생한다() {
        User user = user("kakao-123");
        Group group = group("ABC123", "테스트그룹");
        given(userRepository.findByKakaoId("kakao-123")).willReturn(Optional.of(user));
        given(groupRepository.findByInviteCode("ABC123")).willReturn(Optional.of(group));
        given(groupMemberRepository.existsByGroupGroupIdAndUserUserId(any(), any())).willReturn(true);

        assertThatThrownBy(() -> groupService.joinGroup("kakao-123", new GroupJoinRequest("ABC123")))
            .isInstanceOf(GlobalException.class)
            .satisfies(e -> assertThat(((GlobalException) e).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_JOINED_MEMBER));
    }

    @Test
    void 인증되지_않은_유저면_UNAUTHORIZED_USER_예외가_발생한다() {
        given(userRepository.findByKakaoId("unknown")).willReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.joinGroup("unknown", new GroupJoinRequest("ABC123")))
            .isInstanceOf(AuthException.class)
            .satisfies(e -> assertThat(((AuthException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED_USER));
    }

    // ---- 내 그룹 목록 조회 ----

    @Test
    void 내가_속한_그룹_목록을_성공적으로_조회한다() {
        User user = user("kakao-123");
        Group groupA = group("AAA111", "대학 친구들");
        Group groupB = group("BBB222", "자바 스터디");
        GroupMember memberA = GroupMember.builder().group(groupA).user(user).role(GroupMemberRole.LEADER).build();
        GroupMember memberB = GroupMember.builder().group(groupB).user(user).role(GroupMemberRole.MEMBER).build();

        given(userRepository.findByKakaoId("kakao-123")).willReturn(Optional.of(user));
        given(groupMemberRepository.findActiveGroupsByUserId(any())).willReturn(List.of(memberA, memberB));
        given(groupMemberRepository.countByGroupGroupId(any())).willReturn(4, 6);

        GroupListResponse response = groupService.getMyGroups("kakao-123");

        assertThat(response.groups()).hasSize(2);
        assertThat(response.groups().get(0).groupName()).isEqualTo("대학 친구들");
        assertThat(response.groups().get(0).memberCount()).isEqualTo(4);
        assertThat(response.groups().get(1).groupName()).isEqualTo("자바 스터디");
        assertThat(response.groups().get(1).memberCount()).isEqualTo(6);
    }

    @Test
    void 소프트삭제된_그룹은_목록_조회에서_제외된다() {
        User user = user("kakao-123");
        Group activeGroup = group("AAA111", "살아있는 그룹");
        GroupMember activeMember = GroupMember.builder().group(activeGroup).user(user).role(GroupMemberRole.LEADER).build();

        given(userRepository.findByKakaoId("kakao-123")).willReturn(Optional.of(user));
        // findActiveGroupsByUserId는 deletedAt IS NULL 조건으로 소프트삭제된 그룹을 이미 제외하고 반환
        given(groupMemberRepository.findActiveGroupsByUserId(any())).willReturn(List.of(activeMember));
        given(groupMemberRepository.countByGroupGroupId(any())).willReturn(3);

        GroupListResponse response = groupService.getMyGroups("kakao-123");

        assertThat(response.groups()).hasSize(1);
        assertThat(response.groups().get(0).groupName()).isEqualTo("살아있는 그룹");
    }

    @Test
    void 가입한_그룹이_없으면_빈_목록을_반환한다() {
        given(userRepository.findByKakaoId("kakao-123")).willReturn(Optional.of(user("kakao-123")));
        given(groupMemberRepository.findActiveGroupsByUserId(any())).willReturn(List.of());

        GroupListResponse response = groupService.getMyGroups("kakao-123");

        assertThat(response.groups()).isEmpty();
    }

    @Test
    void 그룹_목록_조회_시_인증되지_않은_유저면_UNAUTHORIZED_USER_예외가_발생한다() {
        given(userRepository.findByKakaoId("unknown")).willReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.getMyGroups("unknown"))
            .isInstanceOf(AuthException.class)
            .satisfies(e -> assertThat(((AuthException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED_USER));
    }

    // ---- 그룹 수정 ----

    @Test
    void LEADER가_그룹_이름을_수정에_성공한다() {
        User user = user("kakao-123");
        Group group = group("ABC123", "대학 친구들");
        GroupMember leader = GroupMember.builder().group(group).user(user).role(GroupMemberRole.LEADER).build();

        given(userRepository.findByKakaoId("kakao-123")).willReturn(Optional.of(user));
        given(groupRepository.findById(1L)).willReturn(Optional.of(group));
        given(groupMemberRepository.findByGroupGroupIdAndUserUserId(1L, user.getUserId())).willReturn(Optional.of(leader));

        GroupUpdateResponse response = groupService.updateGroup("kakao-123", 1L, new GroupUpdateRequest("수정된 이름"));

        assertThat(response.groupName()).isEqualTo("수정된 이름");
    }

    @Test
    void MEMBER가_그룹_수정_시도하면_NOT_GROUP_LEADER_예외가_발생한다() {
        User user = user("kakao-123");
        Group group = group("ABC123", "대학 친구들");
        GroupMember member = GroupMember.builder().group(group).user(user).role(GroupMemberRole.MEMBER).build();

        given(userRepository.findByKakaoId("kakao-123")).willReturn(Optional.of(user));
        given(groupRepository.findById(1L)).willReturn(Optional.of(group));
        given(groupMemberRepository.findByGroupGroupIdAndUserUserId(1L, user.getUserId())).willReturn(Optional.of(member));

        assertThatThrownBy(() -> groupService.updateGroup("kakao-123", 1L, new GroupUpdateRequest("수정된 이름")))
            .isInstanceOf(GlobalException.class)
            .satisfies(e -> assertThat(((GlobalException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_GROUP_LEADER));
    }

    @Test
    void 그룹_수정_시_그룹멤버가_아니면_NOT_GROUP_MEMBER_예외가_발생한다() {
        User user = user("kakao-123");
        Group group = group("ABC123", "대학 친구들");

        given(userRepository.findByKakaoId("kakao-123")).willReturn(Optional.of(user));
        given(groupRepository.findById(1L)).willReturn(Optional.of(group));
        given(groupMemberRepository.findByGroupGroupIdAndUserUserId(1L, user.getUserId())).willReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.updateGroup("kakao-123", 1L, new GroupUpdateRequest("수정된 이름")))
            .isInstanceOf(GlobalException.class)
            .satisfies(e -> assertThat(((GlobalException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_GROUP_MEMBER));
    }

    @Test
    void 그룹_수정_시_존재하지_않는_그룹이면_GROUP_NOT_FOUND_예외가_발생한다() {
        given(userRepository.findByKakaoId("kakao-123")).willReturn(Optional.of(user("kakao-123")));
        given(groupRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.updateGroup("kakao-123", 999L, new GroupUpdateRequest("수정된 이름")))
            .isInstanceOf(GlobalException.class)
            .satisfies(e -> assertThat(((GlobalException) e).getErrorCode())
                .isEqualTo(ErrorCode.GROUP_NOT_FOUND));
    }

    // ---- 그룹 상세 조회 ----

    @Test
    void 그룹_상세_조회에_성공한다() {
        User user = user("kakao-123");
        Group group = group("ABC123", "대학 친구들");
        GroupMember myMember = GroupMember.builder().group(group).user(user).role(GroupMemberRole.LEADER).build();
        Room room = Room.builder().group(group).roomName("강남역 모임").status(RoomStatus.VOTING).build();

        given(userRepository.findByKakaoId("kakao-123")).willReturn(Optional.of(user));
        given(groupRepository.findById(1L)).willReturn(Optional.of(group));
        given(groupMemberRepository.findByGroupGroupIdAndUserUserId(1L, user.getUserId())).willReturn(Optional.of(myMember));
        given(groupMemberRepository.findByGroupGroupId(1L)).willReturn(List.of(myMember));
        given(roomRepository.findByGroupGroupIdAndDeletedAtIsNull(1L)).willReturn(List.of(room));

        GroupDetailResponse response = groupService.getGroupDetail("kakao-123", 1L);

        assertThat(response.groupName()).isEqualTo("대학 친구들");
        assertThat(response.myRole()).isEqualTo(GroupMemberRole.LEADER);
        assertThat(response.members()).hasSize(1);
        assertThat(response.rooms()).hasSize(1);
        assertThat(response.rooms().get(0).roomName()).isEqualTo("강남역 모임");
    }

    @Test
    void 그룹_상세_조회_시_멤버가_아니면_NOT_GROUP_MEMBER_예외가_발생한다() {
        given(userRepository.findByKakaoId("kakao-123")).willReturn(Optional.of(user("kakao-123")));
        given(groupRepository.findById(1L)).willReturn(Optional.of(group("ABC123", "대학 친구들")));
        given(groupMemberRepository.findByGroupGroupIdAndUserUserId(1L, null)).willReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.getGroupDetail("kakao-123", 1L))
            .isInstanceOf(GlobalException.class)
            .satisfies(e -> assertThat(((GlobalException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_GROUP_MEMBER));
    }

    @Test
    void 그룹_상세_조회_시_방이_없으면_빈_목록을_반환한다() {
        User user = user("kakao-123");
        Group group = group("ABC123", "대학 친구들");
        GroupMember myMember = GroupMember.builder().group(group).user(user).role(GroupMemberRole.MEMBER).build();

        given(userRepository.findByKakaoId("kakao-123")).willReturn(Optional.of(user));
        given(groupRepository.findById(1L)).willReturn(Optional.of(group));
        given(groupMemberRepository.findByGroupGroupIdAndUserUserId(1L, user.getUserId())).willReturn(Optional.of(myMember));
        given(groupMemberRepository.findByGroupGroupId(1L)).willReturn(List.of(myMember));
        given(roomRepository.findByGroupGroupIdAndDeletedAtIsNull(1L)).willReturn(List.of());

        GroupDetailResponse response = groupService.getGroupDetail("kakao-123", 1L);

        assertThat(response.rooms()).isEmpty();
    }

    // ---- 그룹 삭제 ----

    @Test
    void LEADER가_그룹_삭제에_성공한다() {
        User user = user("kakao-123");
        Group group = group("ABC123", "대학 친구들");
        GroupMember leader = GroupMember.builder().group(group).user(user).role(GroupMemberRole.LEADER).build();

        given(userRepository.findByKakaoId("kakao-123")).willReturn(Optional.of(user));
        given(groupRepository.findById(1L)).willReturn(Optional.of(group));
        given(groupMemberRepository.findByGroupGroupIdAndUserUserId(1L, user.getUserId())).willReturn(Optional.of(leader));
        given(roomRepository.findByGroupGroupIdAndDeletedAtIsNull(1L)).willReturn(List.of());

        GroupDeleteResponse response = groupService.deleteGroup("kakao-123", 1L);

        assertThat(response.deletedAt()).isNotNull();
        assertThat(group.getDeletedAt()).isNotNull();
        verify(groupMemberRepository).deleteAllByGroupGroupId(1L);
    }

    @Test
    void 그룹_삭제시_하위_방들도_소프트_삭제된다() {
        User user = user("kakao-123");
        Group group = group("ABC123", "대학 친구들");
        GroupMember leader = GroupMember.builder().group(group).user(user).role(GroupMemberRole.LEADER).build();
        Room room1 = room(group, "강남역 모임");
        Room room2 = room(group, "홍대 모임");

        given(userRepository.findByKakaoId("kakao-123")).willReturn(Optional.of(user));
        given(groupRepository.findById(1L)).willReturn(Optional.of(group));
        given(groupMemberRepository.findByGroupGroupIdAndUserUserId(1L, user.getUserId())).willReturn(Optional.of(leader));
        given(roomRepository.findByGroupGroupIdAndDeletedAtIsNull(1L)).willReturn(List.of(room1, room2));

        groupService.deleteGroup("kakao-123", 1L);

        assertThat(room1.getDeletedAt()).isNotNull();
        assertThat(room2.getDeletedAt()).isNotNull();
        verify(groupMemberRepository).deleteAllByGroupGroupId(1L);
    }

    @Test
    void MEMBER가_그룹_삭제_시도하면_NOT_GROUP_LEADER_예외가_발생한다() {
        User user = user("kakao-123");
        Group group = group("ABC123", "대학 친구들");
        GroupMember member = GroupMember.builder().group(group).user(user).role(GroupMemberRole.MEMBER).build();

        given(userRepository.findByKakaoId("kakao-123")).willReturn(Optional.of(user));
        given(groupRepository.findById(1L)).willReturn(Optional.of(group));
        given(groupMemberRepository.findByGroupGroupIdAndUserUserId(1L, user.getUserId())).willReturn(Optional.of(member));

        assertThatThrownBy(() -> groupService.deleteGroup("kakao-123", 1L))
            .isInstanceOf(GlobalException.class)
            .satisfies(e -> assertThat(((GlobalException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_GROUP_LEADER));
    }

    // ---- 그룹 탈퇴 ----

    @Test
    void MEMBER가_그룹_탈퇴에_성공한다() {
        User user = user("kakao-123");
        Group group = group("ABC123", "대학 친구들");
        GroupMember member = GroupMember.builder().group(group).user(user).role(GroupMemberRole.MEMBER).build();

        given(userRepository.findByKakaoId("kakao-123")).willReturn(Optional.of(user));
        given(groupMemberRepository.findByGroupGroupIdAndUserUserId(1L, user.getUserId())).willReturn(Optional.of(member));

        GroupLeaveResponse response = groupService.leaveGroup("kakao-123", 1L);

        assertThat(response.groupId()).isEqualTo(1L);
        verify(groupMemberRepository).delete(member);
    }

    @Test
    void LEADER가_그룹_탈퇴_시도하면_LEADER_CANNOT_LEAVE_예외가_발생한다() {
        User user = user("kakao-123");
        Group group = group("ABC123", "대학 친구들");
        GroupMember leader = GroupMember.builder().group(group).user(user).role(GroupMemberRole.LEADER).build();

        given(userRepository.findByKakaoId("kakao-123")).willReturn(Optional.of(user));
        given(groupMemberRepository.findByGroupGroupIdAndUserUserId(1L, user.getUserId())).willReturn(Optional.of(leader));

        assertThatThrownBy(() -> groupService.leaveGroup("kakao-123", 1L))
            .isInstanceOf(GlobalException.class)
            .satisfies(e -> assertThat(((GlobalException) e).getErrorCode())
                .isEqualTo(ErrorCode.LEADER_CANNOT_LEAVE));
    }

    @Test
    void 그룹_탈퇴_시_그룹멤버가_아니면_NOT_GROUP_MEMBER_예외가_발생한다() {
        given(userRepository.findByKakaoId("kakao-123")).willReturn(Optional.of(user("kakao-123")));
        given(groupMemberRepository.findByGroupGroupIdAndUserUserId(1L, null)).willReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.leaveGroup("kakao-123", 1L))
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

    private Room room(Group group, String roomName) {
        return Room.builder()
            .group(group)
            .roomName(roomName)
            .status(RoomStatus.VOTING)
            .build();
    }
}
