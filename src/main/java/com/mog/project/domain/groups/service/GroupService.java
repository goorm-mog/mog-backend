package com.mog.project.domain.groups.service;

import com.mog.project.domain.groups.dto.request.GroupCreateRequest;
import com.mog.project.domain.groups.dto.response.GroupCreateResponse;
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


import java.security.SecureRandom;          
import lombok.RequiredArgsConstructor;                      
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class GroupService {
    private static final String BASE_URL = "https://mo-ge.site/join?code=";
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int INVITE_CODE_LENGTH = 6;

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
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
}
