package com.mog.project.domain.settlement.service;

import com.mog.project.domain.groups.entity.GroupMember;
import com.mog.project.domain.groups.entity.GroupMemberRole;
import com.mog.project.domain.groups.repository.GroupMemberRepository;
import com.mog.project.domain.notification.entity.NotificationType;
import com.mog.project.domain.notification.service.NotificationService;
import com.mog.project.domain.meeting.entity.MeetingMemberCost;
import com.mog.project.domain.meeting.entity.MeetingRecord;
import com.mog.project.domain.meeting.repository.MeetingMemberCostRepository;
import com.mog.project.domain.meeting.repository.MeetingRecordRepository;
import com.mog.project.domain.room.entity.Room;
import com.mog.project.domain.room.repository.RoomRepository;
import com.mog.project.domain.settlement.dto.response.MemberSettlementResponse;
import com.mog.project.domain.settlement.dto.response.SettlementDetailResponse;
import com.mog.project.domain.settlement.dto.response.SettlementResponse;
import com.mog.project.domain.settlement.entity.MemberSettlement;
import com.mog.project.domain.settlement.entity.Settlement;
import com.mog.project.domain.settlement.repository.MemberSettlementRepository;
import com.mog.project.domain.settlement.repository.SettlementRepository;
import com.mog.project.domain.user.entity.User;
import com.mog.project.domain.user.repository.UserRepository;
import com.mog.project.global.exception.ErrorCode;
import com.mog.project.global.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final MemberSettlementRepository memberSettlementRepository;
    private final MeetingRecordRepository meetingRecordRepository;
    private final MeetingMemberCostRepository meetingMemberCostRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final NotificationService notificationService;

    // 정산 계산 및 생성
    // 방 멤버라면 누구나 호출이 되고, 이미 존재하면 삭제 후 재생성
    @Transactional
    public SettlementResponse calculateAndCreate(Long roomId, String kakaoId) {

        // 방을 우선 조회 (없으면 ROOM_NOT_FOUND 반환)
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new GlobalException(ErrorCode.ROOM_NOT_FOUND));

        // kakaoId로 User 조회 후 userId 추출
        User user = userRepository.findByKakaoId(kakaoId)
                .orElseThrow(() -> new GlobalException(ErrorCode.FORBIDDEN));

        // 현재 유저의 groupMemberId를 조회
        // Room -> Group -> GroupMember로 조회
        Long groupId = room.getGroup().getGroupId();
        GroupMember currentMember = groupMemberRepository.findByGroupGroupIdAndUserUserId(groupId, user.getUserId())
                .orElseThrow(() -> new GlobalException(ErrorCode.FORBIDDEN));

        // 해당 방의 모든 차수 기록을 조회
        List<MeetingRecord> records = meetingRecordRepository.findByRoomId(roomId);

        // 만약 조회 결과가 없으면 NO_RECORDS 반환
        if(records.isEmpty()) {
            throw new GlobalException(ErrorCode.NO_RECORDS);
        }

        // 모든 차수의 참여자 비용을 조회
        List<MeetingMemberCost> allCosts = meetingMemberCostRepository.findByMeetingRecordIn(records);

        // 전체 총 비용 계산
        int totalCost = allCosts.stream().mapToInt(MeetingMemberCost::getAmount).sum();

        // roomMemberId를 기준으로 비용을 그룹핑 (멤버별 차수 상세 내역)
        Map<Long, List<MeetingMemberCost>> costsByMember = allCosts.stream()
                .collect(Collectors.groupingBy(MeetingMemberCost::getRoomMemberId));

        // 이미 정산이 존재한다면 삭제 후 재생성
        settlementRepository.findByRoomId(roomId).ifPresent(settlementRepository::delete);
        settlementRepository.flush();

        // 정산 생성
        Settlement settlement = Settlement.builder()
                .roomId(roomId)
                .createdBy(currentMember.getGroupMemberId())
                .totalCost(totalCost)
                .build();
        settlementRepository.save(settlement);

        // 멤버별 총 부담액 계산 및 MemberSettlement 생성
        for (Map.Entry<Long, List<MeetingMemberCost>> entry : costsByMember.entrySet()) {
            Long memberId = entry.getKey();
            int memberTotal = entry.getValue().stream().mapToInt(MeetingMemberCost::getAmount).sum();

            MemberSettlement memberSettlement = MemberSettlement.builder()
                    .settlement(settlement)
                    .roomMemberId(memberId)
                    .totalAmount(memberTotal)
                    .build();
            settlement.addMemberSettlement(memberSettlement);
        }
        Map<Long, String> nicknameMap = buildNicknameMap(groupId);
        return buildResponse(settlement, costsByMember, nicknameMap);
    }

    public SettlementResponse getSettlement(Long roomId, String kakaoId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new GlobalException(ErrorCode.ROOM_NOT_FOUND));

        User user = userRepository.findByKakaoId(kakaoId)
                .orElseThrow(() -> new GlobalException(ErrorCode.FORBIDDEN));
        groupMemberRepository.findByGroupGroupIdAndUserUserId(room.getGroup().getGroupId(), user.getUserId())
                .orElseThrow(() -> new GlobalException(ErrorCode.FORBIDDEN));

        Settlement settlement = settlementRepository.findByRoomId(roomId)
                .orElseThrow(() -> new GlobalException(ErrorCode.SETTLEMENT_NOT_FOUND));

        List<MeetingRecord> records = meetingRecordRepository.findByRoomId(roomId);
        List<MeetingMemberCost> allCosts = meetingMemberCostRepository.findByMeetingRecordIn(records);

        Map<Long, List<MeetingMemberCost>> costsByMember = allCosts.stream()
                .collect(Collectors.groupingBy(MeetingMemberCost::getRoomMemberId));

        Long groupId = room.getGroup().getGroupId();
        Map<Long, String> nicknameMap = buildNicknameMap(groupId);

        return buildResponse(settlement, costsByMember, nicknameMap);
    }

    // 정산 확정
    // 방장만 가능
    @Transactional
    public SettlementResponse confirmSettlement(Long roomId, String kakaoId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new GlobalException(ErrorCode.ROOM_NOT_FOUND));

        Settlement settlement = settlementRepository.findByRoomId(roomId)
                .orElseThrow(() -> new GlobalException(ErrorCode.SETTLEMENT_NOT_FOUND));

        if (settlement.getIsConfirmed()) {
            throw new GlobalException(ErrorCode.ALREADY_CONFIRMED);
        }

        // kakaoId로 User 조회 후 userId 추출
        User user = userRepository.findByKakaoId(kakaoId)
                .orElseThrow(() -> new GlobalException(ErrorCode.FORBIDDEN));

        Long groupId = room.getGroup().getGroupId();

        GroupMember currentMember = groupMemberRepository
                .findByGroupGroupIdAndUserUserId(groupId, user.getUserId())
                .orElseThrow(() -> new GlobalException(ErrorCode.FORBIDDEN));

        if (currentMember.getRole() != GroupMemberRole.LEADER) {
            throw new GlobalException(ErrorCode.NOT_HOST);
        }

        settlement.confirm();

        String message = "[" + room.getRoomName() + "] 정산이 완료됐습니다.";
        groupMemberRepository.findByGroupGroupId(groupId)
                .forEach(gm -> notificationService.send(
                        gm.getUser().getUserId(),
                        NotificationType.SETTLEMENT_DONE,
                        message,
                        roomId
                ));

        List<MeetingRecord> records = meetingRecordRepository.findByRoomId(roomId);
        List<MeetingMemberCost> allCosts = meetingMemberCostRepository.findByMeetingRecordIn(records);
        Map<Long, List<MeetingMemberCost>> costsByMember = allCosts.stream()
                .collect(Collectors.groupingBy(MeetingMemberCost::getRoomMemberId));
        Map<Long, String> nicknameMap = buildNicknameMap(groupId);

        return buildResponse(settlement, costsByMember, nicknameMap);
    }

    // 닉네임 매핑
    private Map<Long, String> buildNicknameMap(Long groupId) {
        return groupMemberRepository.findByGroupGroupId(groupId).stream()
                .collect(Collectors.toMap(
                        GroupMember::getGroupMemberId,
                        gm -> gm.getUser().getNickname()
                ));
    }

    // settlement를 settlementResponse로 반환
    private SettlementResponse buildResponse(
            Settlement settlement,
            Map<Long, List<MeetingMemberCost>> costsByMember,
            Map<Long, String> nicknameMap
    ) {
        List<MemberSettlementResponse> memberResponse = settlement.getMemberSettlements().stream()
                .map(ms -> {
                    List<MeetingMemberCost> costs = costsByMember.getOrDefault(ms.getRoomMemberId(), List.of());
                    List<SettlementDetailResponse> details = costs.stream()
                            .map(cost -> SettlementDetailResponse.from(cost, nicknameMap))
                            .toList();
                    String nickname = nicknameMap.get(ms.getRoomMemberId());
                    return MemberSettlementResponse.from(ms, nickname, details);
                })
                .toList();
        return SettlementResponse.from(settlement, memberResponse);
    }




}
