package com.mog.project.domain.room.service;

import com.mog.project.domain.groups.entity.Group;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoomService {

   private final RoomRepository roomRepository;
   private final GroupRepository groupRepository;
   private final GroupMemberRepository groupMemberRepository;
   private final UserRepository userRepository;

   @Transactional
   public RoomCreateResponse createRoom(String kakaoId, Long groupId, RoomCreateRequest request) {
      User user = userRepository.findByKakaoId(kakaoId)
         .orElseThrow(() -> new AuthException(ErrorCode.UNAUTHORIZED_USER));
      
      Group group = groupRepository.findById(groupId)
         .orElseThrow(() -> new GlobalException(ErrorCode.GROUP_NOT_FOUND));
      
      groupMemberRepository.findByGroupGroupIdAndUserUserId(groupId, user.getUserId())
         .orElseThrow(() -> new GlobalException(ErrorCode.NOT_GROUP_MEMBER));

      Room room = Room.builder()
         .group(group)
         .creator(user)
         .roomName(request.roomName())
         .status(RoomStatus.VOTING)
         .build();
      roomRepository.save(room);

      return new RoomCreateResponse(
              room.getRoomId(),
              group.getGroupId(),
              room.getRoomName(),
              room.getStatus(),
              user.getUserId(),
              room.getCreatedAt()
           );
   }
}