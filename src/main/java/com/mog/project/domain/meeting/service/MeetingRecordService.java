package com.mog.project.domain.meeting.service;

import com.mog.project.domain.notification.entity.NotificationType;
import com.mog.project.domain.notification.service.NotificationService;
import com.mog.project.global.exception.ErrorCode;
import com.mog.project.global.exception.GlobalException;
import com.mog.project.domain.meeting.dto.request.MeetingRecordCreateRequest;
import com.mog.project.domain.meeting.dto.request.MeetingRecordUpdateRequest;
import com.mog.project.domain.meeting.dto.response.MeetingRecordListResponse;
import com.mog.project.domain.meeting.dto.response.MeetingRecordResponse;
import com.mog.project.domain.meeting.dto.response.RoomPhotoResponse;
import com.mog.project.domain.meeting.entity.MeetingMemberCost;
import com.mog.project.domain.meeting.entity.MeetingRecord;
import com.mog.project.domain.meeting.repository.MeetingMemberCostRepository;
import com.mog.project.domain.meeting.repository.MeetingRecordRepository;
import com.mog.project.domain.groups.entity.GroupMember;
import com.mog.project.domain.groups.repository.GroupMemberRepository;
import com.mog.project.domain.room.entity.Room;
import com.mog.project.domain.room.repository.RoomRepository;
import com.mog.project.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingRecordService {

    private final MeetingRecordRepository meetingRecordRepository;
    private final MeetingMemberCostRepository meetingMemberCostRepository;
    private final RoomPhotoService roomPhotoService;
    private final RoomRepository roomRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // 해당 방의 전체 차수 기록 + 사진 조회
    public MeetingRecordListResponse getRecords(Long roomId, String kakaoId) {
        checkMembership(roomId, kakaoId);

        // 모든 차수의 목록을 조회
        List<MeetingRecord> records = meetingRecordRepository.findByRoomId(roomId);

        // 모든 차수의 참여자 비용을 조회
        List<MeetingMemberCost> allCosts = meetingMemberCostRepository.findByMeetingRecordIn(records);

        List<RoomPhotoResponse> photos = roomPhotoService.getPhotos(roomId);

        Map<Long, String> nicknameMap = buildNicknameMap(roomId);

        // response DTO에 담아서 조회 결과를 제공
        return MeetingRecordListResponse.from(photos, records, allCosts, nicknameMap);
    }

    // 새 차수 추가
    @Transactional
    public MeetingRecordResponse createRecord(Long roomId, String kakaoId, MeetingRecordCreateRequest request) {
        checkMembership(roomId, kakaoId);

        // 현재 방의 차수를 자동으로 + 1
        int nextSeq = meetingRecordRepository.findMaxSeqByRoomId(roomId) + 1;

        // MeetingRecord Entity를 생성하는데
        // MeetingRecordCreateRequest 결과를 받기때문에 그 결과를 넣어줌
        MeetingRecord record = MeetingRecord.builder()
                .roomId(roomId)
                .seq(nextSeq)
                .placeName(request.placeName())
                .memo(request.memo())
                .payerRoomMemberId(request.payer() != null ? request.payer().roomMemberId() : null)
                .payerBankName(request.payer() != null ? request.payer().bankName() : null)
                .payerAccountNumber(request.payer() != null ? request.payer().accountNumber() : null)
                .build();
        meetingRecordRepository.save(record);

        // participants 목록을 엔티티로 변환
        List<MeetingMemberCost> costs = request.participants().stream()
                .map(p -> MeetingMemberCost.builder()
                        .meetingRecord(record)
                        .roomMemberId(p.roomMemberId())
                        .amount(p.amount())
                        .build())
                .toList();
        meetingMemberCostRepository.saveAll(costs);

        // 저장된 엔티티를 응답 DTO로 변환
        Map<Long, String> nicknameMap = buildNicknameMap(roomId);

        // 방 멤버 전체에게 알림을 전송
        notifyRoomMembers(roomId, nextSeq);

        return MeetingRecordResponse.from(record, costs, nicknameMap);
    }

    // 특정 차수 수정
    @Transactional
    public MeetingRecordResponse updateRecord(Long roomId, Long recordId, String kakaoId, MeetingRecordUpdateRequest request) {
        checkMembership(roomId, kakaoId);

        // 다른방 기록 수정 방지를 위해 recordId와 roomId로 조회
        MeetingRecord record = meetingRecordRepository.findByIdAndRoomId(recordId, roomId)
                .orElseThrow(() -> new GlobalException(ErrorCode.RECORD_NOT_FOUND));

        // 엔티티의 update() 메서드 호출, PATCH 방식으로 구현
        record.update(
                request.placeName(),
                request.memo(),
                request.payer() != null ? request.payer().roomMemberId() : null,
                request.payer() != null ? request.payer().bankName() : null,
                request.payer() != null ? request.payer().accountNumber() : null
        );

        // 기존 데이터를 삭제하고 새로 저장하는 방식
        List<MeetingMemberCost> costs;
        if (request.participants() != null) {
            meetingMemberCostRepository.deleteByMeetingRecord(record);
            costs = request.participants().stream()
                    .map(p -> MeetingMemberCost.builder()
                            .meetingRecord(record)
                            .roomMemberId(p.roomMemberId())
                            .amount(p.amount())
                            .build())
                    .toList();
            meetingMemberCostRepository.saveAll(costs);
        } else {
            costs = meetingMemberCostRepository.findByMeetingRecordIn(List.of(record));
        }

        Map<Long, String> nicknameMap = buildNicknameMap(roomId);
        return MeetingRecordResponse.from(record, costs, nicknameMap);
    }

    // 특정 차수 삭제
    @Transactional
    public void deleteRecord(Long roomId, Long recordId, String kakaoId) {
        checkMembership(roomId, kakaoId);

        // 레포지토리에서 조회, roomId로 이중 검증
        MeetingRecord record = meetingRecordRepository.findByIdAndRoomId(recordId, roomId)
                .orElseThrow(() -> new GlobalException(ErrorCode.RECORD_NOT_FOUND));

        // 삭제 전 차수를 저장
        int seq = record.getSeq();

        // FK가 있는 자식을 먼저 삭제 후 부모 삭제
        meetingMemberCostRepository.deleteByMeetingRecord(record);
        meetingRecordRepository.delete(record);

        // 삭제된 차수들을 -1만큼씩 땡김
        meetingRecordRepository.decreaseSeqAfter(roomId, seq);
    }

    private void checkMembership(Long roomId, String kakaoId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new GlobalException(ErrorCode.ROOM_NOT_FOUND));
        var user = userRepository.findByKakaoId(kakaoId)
                .orElseThrow(() -> new GlobalException(ErrorCode.FORBIDDEN));
        groupMemberRepository.findByGroupGroupIdAndUserUserId(room.getGroup().getGroupId(), user.getUserId())
                .orElseThrow(() -> new GlobalException(ErrorCode.FORBIDDEN));
    }

    private Map<Long, String> buildNicknameMap(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new GlobalException(ErrorCode.ROOM_NOT_FOUND));
        Long groupId = room.getGroup().getGroupId();
        return groupMemberRepository.findByGroupGroupId(groupId).stream()
                .collect(Collectors.toMap(
                        GroupMember::getGroupMemberId,
                        gm -> gm.getUser().getNickname()
                ));
    }

    // 알림 발송 메서드
    private void notifyRoomMembers(Long roomId, int seq) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new GlobalException(ErrorCode.ROOM_NOT_FOUND));
        String message = "[" + room.getRoomName() + "]" + seq + "차 기록이 추가됐습니다.";
        groupMemberRepository.findByGroupGroupId(room.getGroup().getGroupId())
                .forEach(gm -> notificationService.send(
                        gm.getUser().getUserId(),
                        NotificationType.RECORD_ADDED,
                        message,
                        roomId
                ));
    }

}
