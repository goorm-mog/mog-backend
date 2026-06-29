package com.mog.project.domain.groups.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.mog.project.domain.groups.dto.request.GroupJoinRequest;
import com.mog.project.domain.groups.dto.response.GroupJoinResponse;
import com.mog.project.domain.groups.entity.Group;
import com.mog.project.domain.groups.entity.GroupMember;
import com.mog.project.domain.groups.entity.GroupMemberRole;
import com.mog.project.domain.groups.repository.GroupMemberRepository;
import com.mog.project.domain.groups.repository.GroupRepository;
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
class GroupServiceTest {

    @Mock private GroupRepository groupRepository;
    @Mock private GroupMemberRepository groupMemberRepository;
    @Mock private UserRepository userRepository;

    private GroupService groupService;

    @BeforeEach
    void setUp() {
        groupService = new GroupService(groupRepository, groupMemberRepository, userRepository);
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
