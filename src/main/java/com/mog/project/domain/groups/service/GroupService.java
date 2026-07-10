package com.mog.project.domain.groups.service;

import com.mog.project.domain.groups.dto.request.GroupCreateRequest;
import com.mog.project.domain.groups.dto.response.GroupCreateResponse;
import com.mog.project.domain.groups.dto.request.GroupJoinRequest;
import com.mog.project.domain.groups.dto.response.GroupJoinResponse;
import com.mog.project.domain.groups.dto.response.GroupListResponse;
import com.mog.project.domain.groups.dto.request.GroupUpdateRequest;
import com.mog.project.domain.groups.dto.response.GroupUpdateResponse;
import com.mog.project.domain.groups.dto.response.GroupDeleteResponse;
import com.mog.project.domain.groups.dto.response.GroupDetailResponse;
import com.mog.project.domain.groups.dto.response.GroupLeaveResponse;
import com.mog.project.domain.room.entity.Room;
import com.mog.project.domain.room.repository.RoomRepository;
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

import java.security.SecureRandom;          
import lombok.RequiredArgsConstructor;                      
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupService {
    private static final String BASE_URL = "https://mo-ge.site/join?code=";
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int INVITE_CODE_LENGTH = 6;

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    // 초대 코드 생성 시 중복 방지를 위해 SecureRandom 사용
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public GroupCreateResponse createGroup(String kakaoId, GroupCreateRequest request) {
        User user = userRepository.findByKakaoId(kakaoId)
            .orElseThrow(() -> new AuthException(ErrorCode.UNAUTHORIZED_USER));

            String inviteCode = generateUniqueInviteCode();
            String kakaoShareUrl = BASE_URL + inviteCode;

            Group group = Group.builder()
                .groupName(request.groupName())
                .inviteCode(inviteCode)
                .kakaoShareUrl(kakaoShareUrl)
                .build();
            groupRepository.save(group);

            GroupMember groupMember = GroupMember.builder()
                .group(group)
                .user(user)
                .role(GroupMemberRole.LEADER)
                .build();
            groupMemberRepository.save(groupMember);

            return new GroupCreateResponse(
                group.getGroupId(),
                group.getGroupName(),
                group.getInviteCode(),
                group.getKakaoShareUrl(),
                group.getCreatedAt()
            );
    }


    @Transactional
    public GroupJoinResponse joinGroup(String kakaoId, GroupJoinRequest request) {
        User user = userRepository.findByKakaoId(kakaoId)
            .orElseThrow(() -> new AuthException(ErrorCode.UNAUTHORIZED_USER));

        Group group = groupRepository.findByInviteCode(request.inviteCode())
            .orElseThrow(() -> new GlobalException(ErrorCode.GROUP_NOT_FOUND));

        if (groupMemberRepository.existsByGroupGroupIdAndUserUserId(group.getGroupId(), user.getUserId())) {
            throw new GlobalException(ErrorCode.ALREADY_JOINED_MEMBER);
        }

        GroupMember groupMember = GroupMember.builder()
            .group(group)
            .user(user)
            .role(GroupMemberRole.MEMBER)
            .build();
        groupMemberRepository.save(groupMember);

        return new GroupJoinResponse(
            group.getGroupId(),
            group.getGroupName(),
            GroupMemberRole.MEMBER
        );
    }

    // 초대 코드 생성 시 중복 방지를 위해 고유한 초대 코드를 생성하는 메서드
    private String generateUniqueInviteCode() {
        String code;
        do {
            code = generateInviteCode();
        } while (groupRepository.existsByInviteCode(code));
        return code;
    }

    private String generateInviteCode() {
        StringBuilder sb = new StringBuilder(INVITE_CODE_LENGTH);
        for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
            sb.append(CHARACTERS.charAt(secureRandom.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public GroupListResponse getMyGroups(String kakaoId) {
        User user = userRepository.findByKakaoId(kakaoId)
          .orElseThrow(() -> new AuthException(ErrorCode.UNAUTHORIZED_USER));
    
        List<GroupListResponse.GroupItemResponse> groups = groupMemberRepository.findActiveGroupsByUserId(user.getUserId())
        .stream()
        .map(gm -> new GroupListResponse.GroupItemResponse(
            gm.getGroup().getGroupId(),
            gm.getGroup().getGroupName(),
            groupMemberRepository.countByGroupGroupId(gm.getGroup().getGroupId())
        ))
        .toList();

        return new GroupListResponse(groups);
        }

     @Transactional
     public GroupUpdateResponse updateGroup(String kakaoId, Long groupId, GroupUpdateRequest request) {
        User user = userRepository.findByKakaoId(kakaoId)
          .orElseThrow(() -> new AuthException(ErrorCode.UNAUTHORIZED_USER));

        Group group = groupRepository.findById(groupId)
          .orElseThrow(() -> new GlobalException(ErrorCode.GROUP_NOT_FOUND));

        GroupMember groupMember = groupMemberRepository.findByGroupGroupIdAndUserUserId(groupId, user.getUserId())
          .orElseThrow(() -> new GlobalException(ErrorCode.NOT_GROUP_MEMBER));

        if (groupMember.getRole() != GroupMemberRole.LEADER) {
            throw new GlobalException(ErrorCode.NOT_GROUP_LEADER);
        }

        group.updateName(request.groupName());

        return new GroupUpdateResponse(
            group.getGroupId(),
            group.getGroupName(),
            group.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public GroupDetailResponse getGroupDetail(String kakaoId, Long groupId) {
        User user = userRepository.findByKakaoId(kakaoId)
            .orElseThrow(() -> new AuthException(ErrorCode.UNAUTHORIZED_USER));

        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new GlobalException(ErrorCode.GROUP_NOT_FOUND));

        GroupMember myMembership = groupMemberRepository.findByGroupGroupIdAndUserUserId(groupId, user.getUserId())
            .orElseThrow(() -> new GlobalException(ErrorCode.NOT_GROUP_MEMBER));

        List<GroupDetailResponse.MemberInfo> members = groupMemberRepository.findByGroupGroupId(groupId)
            .stream()
            .map(gm -> new GroupDetailResponse.MemberInfo(
                gm.getUser().getUserId(),
                gm.getUser().getNickname(),
                gm.getRole()
            ))
            .toList();

        List<GroupDetailResponse.RoomInfo> rooms = roomRepository.findByGroupGroupIdAndDeletedAtIsNull(groupId)
            .stream()
            .map(r -> new GroupDetailResponse.RoomInfo(
                r.getRoomId(),
                r.getRoomName(),
                r.getStatus(),
                r.getPromiseDate()
            ))
            .toList();

        return new GroupDetailResponse(
            group.getGroupId(),
            group.getGroupName(),
            group.getInviteCode(),
            myMembership.getRole(),
            members,
            rooms
        );
    }

    @Transactional
    public GroupDeleteResponse deleteGroup(String kakaoId, Long groupId) {
        User user = userRepository.findByKakaoId(kakaoId)
            .orElseThrow(() -> new AuthException(ErrorCode.UNAUTHORIZED_USER));

        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new GlobalException(ErrorCode.GROUP_NOT_FOUND));

        GroupMember groupMember = groupMemberRepository.findByGroupGroupIdAndUserUserId(groupId, user.getUserId())
            .orElseThrow(() -> new GlobalException(ErrorCode.NOT_GROUP_MEMBER));

        if (groupMember.getRole() != GroupMemberRole.LEADER) {
            throw new GlobalException(ErrorCode.NOT_GROUP_LEADER);
        }

        roomRepository.findByGroupGroupIdAndDeletedAtIsNull(groupId)
            .forEach(Room::softDelete);
        groupMemberRepository.deleteAllByGroupGroupId(groupId);
        group.softDelete();

        return new GroupDeleteResponse(group.getGroupId(), group.getDeletedAt());
    }

    @Transactional
    public GroupLeaveResponse leaveGroup(String kakaoId, Long groupId) {
        User user = userRepository.findByKakaoId(kakaoId)
            .orElseThrow(() -> new AuthException(ErrorCode.UNAUTHORIZED_USER));

        GroupMember groupMember = groupMemberRepository.findByGroupGroupIdAndUserUserId(groupId, user.getUserId())
            .orElseThrow(() -> new GlobalException(ErrorCode.NOT_GROUP_MEMBER));

        if (groupMember.getRole() == GroupMemberRole.LEADER) {
            throw new GlobalException(ErrorCode.LEADER_CANNOT_LEAVE);
        }

        groupMemberRepository.delete(groupMember);

        return new GroupLeaveResponse(groupId, user.getUserId());
    }
}
