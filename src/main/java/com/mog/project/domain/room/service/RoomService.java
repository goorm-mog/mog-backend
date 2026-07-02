package com.mog.project.domain.room.service;

import com.mog.project.domain.groups.entity.Group;
import com.mog.project.domain.groups.repository.GroupMemberRepository;
import com.mog.project.domain.groups.repository.GroupRepository;
import com.mog.project.domain.meeting.repository.MeetingRecordRepository;
import com.mog.project.domain.room.dto.request.RoomCreateRequest;
import com.mog.project.domain.room.dto.response.RoomCreateResponse;
import com.mog.project.domain.room.dto.response.RoomListResponse;
import com.mog.project.domain.room.dto.response.RoomStatusResponse;
import com.mog.project.domain.room.entity.Room;
import com.mog.project.domain.room.entity.RoomStatus;
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

@Service
@RequiredArgsConstructor
public class RoomService {

   private final RoomRepository roomRepository;
   private final GroupRepository groupRepository;
   private final GroupMemberRepository groupMemberRepository;
   private final UserRepository userRepository;
   private final RoomMemberRepository roomMemberRepository;
   private final MeetingRecordRepository meetingRecordRepository;

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

   @Transactional(readOnly = true)
   public RoomListResponse getRoomList(String kakaoId, Long groupId) {
      User user = userRepository.findByKakaoId(kakaoId)
         .orElseThrow(() -> new AuthException(ErrorCode.UNAUTHORIZED_USER));
      
      groupMemberRepository.findByGroupGroupIdAndUserUserId(groupId, user.getUserId())
         .orElseThrow(() -> new GlobalException(ErrorCode.NOT_GROUP_MEMBER));
      
      List<RoomListResponse.RoomInfo> rooms = roomRepository.findByGroupGroupIdAndDeletedAtIsNull(groupId)
          .stream()
          .map(room -> new RoomListResponse.RoomInfo(
            room.getRoomId(),
            room.getRoomName(),
            room.getStatus(),
            room.getCreatedAt()
          ))
          .toList();

      return new RoomListResponse(rooms);
   }

   @Transactional(readOnly = true)
   public RoomStatusResponse getRoomStatus(Long roomId) {
      Room room = roomRepository.findById(roomId)
           .filter(r -> r.getDeletedAt() == null)
           .orElseThrow(() -> new GlobalException(ErrorCode.ROOM_NOT_FOUND));
      
      int currentStep = meetingRecordRepository.findMaxSeqByRoomId(roomId);

      List<RoomStatusResponse.MemberInfo> members = roomMemberRepository.findByRoomRoomId(roomId)
          .stream()
          .map(rm -> new RoomStatusResponse.MemberInfo(
               rm.getUser().getUserId(),
               rm.getUser().getNickname(),
               rm.isJoined()
            ))
            .toList();
      return new RoomStatusResponse(
         room.getRoomId(),
         room.getRoomName(),
         room.getStatus(),
         currentStep,
         members
      );
   }
}