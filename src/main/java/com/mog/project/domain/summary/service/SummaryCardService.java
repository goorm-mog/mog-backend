package com.mog.project.domain.summary.service;

import com.mog.project.domain.groups.entity.GroupMember;
import com.mog.project.domain.groups.repository.GroupMemberRepository;
import com.mog.project.domain.meeting.entity.MeetingMemberCost;
import com.mog.project.domain.meeting.entity.MeetingRecord;
import com.mog.project.domain.meeting.entity.RoomPhoto;
import com.mog.project.domain.meeting.repository.MeetingMemberCostRepository;
import com.mog.project.domain.meeting.repository.MeetingRecordRepository;
import com.mog.project.domain.meeting.repository.RoomPhotoRepository;
import com.mog.project.domain.midpoint.repository.ConfirmedPlaceRepository;
import com.mog.project.domain.room.entity.Room;
import com.mog.project.domain.room.repository.RoomRepository;
import com.mog.project.domain.schedule.repository.ConfirmedScheduleRepository;
import com.mog.project.domain.settlement.entity.Settlement;
import com.mog.project.domain.settlement.repository.MemberSettlementRepository;
import com.mog.project.domain.settlement.repository.SettlementRepository;
import com.mog.project.domain.notification.entity.NotificationType;
import com.mog.project.domain.notification.service.NotificationService;
import com.mog.project.domain.summary.dto.response.*;
import com.mog.project.domain.summary.entity.SummaryCard;
import com.mog.project.domain.summary.repository.SummaryCardRepository;
import com.mog.project.domain.user.entity.User;
import com.mog.project.domain.user.repository.UserRepository;
import com.mog.project.global.config.S3Service;
import com.mog.project.global.exception.ErrorCode;
import com.mog.project.global.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SummaryCardService {

    private final RoomRepository roomRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final ConfirmedScheduleRepository confirmedScheduleRepository;
    private final MeetingRecordRepository meetingRecordRepository;
    private final MeetingMemberCostRepository meetingMemberCostRepository;
    private final RoomPhotoRepository roomPhotoRepository;
    private final SettlementRepository settlementRepository;
    private final MemberSettlementRepository memberSettlementRepository;
    private final SummaryCardRepository summaryCardRepository;
    private final S3Service s3Service;
    private final NotificationService notificationService;
    private final ConfirmedPlaceRepository confirmedPlaceRepository;

    public SummaryCardResponse getSummaryData(Long roomId, String kakaoId) {
        Room room = getRoom(roomId);
        Long groupId = room.getGroup().getGroupId();

        // 방 멤버인지 확인
        checkMembership(groupId, kakaoId);

        // 정산이 확정되었는지 확인
        Settlement settlement = getConfirmedSettlement(roomId);

        // roomMemberId -> nickname 매핑
        Map<Long, String> nicknameMap = buildNicknameMap(groupId);

        // 전체 멤버 닉네임 목록 조회
        List<String> members = groupMemberRepository.findByGroupGroupId(groupId).stream()
                .map(gm -> gm.getUser().getNickname())
                .toList();

        // 확정 일정 날짜
        LocalDate confirmedDate = confirmedScheduleRepository.findByRoomId(roomId)
                .map(cs -> cs.getConfirmedDate())
                .orElse(null);

        // 방 사진 URL 목록
        List<String> photos = roomPhotoRepository.findByRoomId(roomId).stream()
                .map(RoomPhoto::getS3Url)
                .toList();

        // 차수별 기록
        List<SummaryRecordResponse> records = buildRecordResponse(roomId, nicknameMap);

        // 정산 요약
        SummarySettlementResponse settlementResponse = buildSettlementResponse(settlement, nicknameMap);

        // 저장된 카드 이미지 URL (없으면 null)
        String cardImageUrl = summaryCardRepository.findByRoomId(roomId)
                .map(SummaryCard::getS3Url)
                .orElse(null);

        SummaryPlaceResponse confirmedPlace = confirmedPlaceRepository.findByRoomId(roomId)
                .map(cp -> new SummaryPlaceResponse(cp.getPlaceName(), cp.getAddress()))
                .orElse(null);

        return new SummaryCardResponse(
                roomId,
                confirmedDate,
                confirmedPlace, // 중간지점 API 구현 후 연동
                members.size(),
                members,
                photos,
                records,
                settlementResponse,
                cardImageUrl
        );
    }

    @Transactional
    public CardImageResponse saveCardImage(Long roomId, String kakaoId, MultipartFile image) {
        Room room = getRoom(roomId);
        Long groupId = room.getGroup().getGroupId();
        checkMembership(groupId, kakaoId);

        Settlement settlement = getConfirmedSettlement(roomId);
        validateImage(image);

        String s3Url = s3Service.upload(image, "cards/room/" + roomId);

        summaryCardRepository.findByRoomId(roomId).ifPresentOrElse(
                card -> {
                    s3Service.delete(card.getS3Url());
                    card.updateS3Url(s3Url);
                },
                () -> summaryCardRepository.save(SummaryCard.builder()
                        .roomId(roomId)
                        .settlementId(settlement.getId())
                        .s3Url(s3Url)
                        .build()
                )
        );

        String message = "[" + room.getRoomName() + "] 요약 카드가 생성됐습니다.";
        groupMemberRepository.findByGroupGroupId(groupId)
                .forEach(gm -> notificationService.send(
                        gm.getUser().getUserId(),
                        NotificationType.SUMMARY_READY,
                        message,
                        roomId
                ));

        return new CardImageResponse(s3Url);
    }

    // room이 존재하는지 확인하는 함수
    private Room getRoom(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new GlobalException(ErrorCode.ROOM_NOT_FOUND));
    }

    // 멤버가 그룹에 속해있는지 확인
    private void checkMembership(Long groupId, String kakaoId) {
        User user = userRepository.findByKakaoId(kakaoId)
                .orElseThrow(() -> new GlobalException(ErrorCode.FORBIDDEN));
        groupMemberRepository.findByGroupGroupIdAndUserUserId(groupId, user.getUserId())
                .orElseThrow(() -> new GlobalException(ErrorCode.FORBIDDEN));
    }

    // 정산이 끝났는지 확인
    private Settlement getConfirmedSettlement(Long roomId) {
        Settlement settlement = settlementRepository.findByRoomId(roomId)
                .orElseThrow(() -> new GlobalException(ErrorCode.SETTLEMENT_NOT_FOUND));
        if (!settlement.getIsConfirmed()) {
            throw new GlobalException(ErrorCode.SETTLEMENT_NOT_CONFIRMED);
        }
        return settlement;
    }

    // groupMemberId로 nickname 생성
    private Map<Long, String> buildNicknameMap(Long groupId) {
        return groupMemberRepository.findByGroupGroupId(groupId).stream()
                .collect(Collectors.toMap(
                        GroupMember::getGroupMemberId,
                        gm -> gm.getUser().getNickname()
                ));
    }

    // 차수 기록 목록 -> DTO로 반환
    private List<SummaryRecordResponse> buildRecordResponse(Long roomId, Map<Long, String> nicknameMap) {
        List<MeetingRecord> records = meetingRecordRepository.findByRoomId(roomId)
                .stream()
                .sorted(Comparator.comparing(MeetingRecord::getSeq))
                .toList();

        Map<Long,List<MeetingMemberCost>> costsByRecord = meetingMemberCostRepository.findByMeetingRecordIn(records).stream()
                .collect(Collectors.groupingBy(c -> c.getMeetingRecord().getId()));

        return records.stream().map(record -> {
            List<MeetingMemberCost> costs = costsByRecord.getOrDefault(record.getId(), List.of());
            int totalCost = costs.stream().mapToInt(MeetingMemberCost::getAmount).sum();
            List<SummaryParticipantResponse> participants = costs.stream()
                    .map(c -> new SummaryParticipantResponse(
                            nicknameMap.getOrDefault(c.getRoomMemberId(), "알수없음"),
                            c.getAmount()
                    ))
                    .toList();
            return new SummaryRecordResponse(record.getSeq(), record.getPlaceName(), record.getMemo(), totalCost, participants);
        }).toList();
    }

    // 정산 요약 → DTO 변환
    private SummarySettlementResponse buildSettlementResponse(Settlement settlement, Map<Long, String> nicknameMap) {
        List<SummaryMemberTotalResponse> memberTotals = memberSettlementRepository.findBySettlement(settlement)
                .stream()
                .map(ms -> new SummaryMemberTotalResponse(
                        nicknameMap.getOrDefault(ms.getRoomMemberId(), "알 수 없음"),
                        ms.getTotalAmount()))
                .toList();
        return new SummarySettlementResponse(settlement.getTotalCost(), memberTotals);
    }

    // png 형식
    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new GlobalException(ErrorCode.INVALID_IMAGE);
        }
        if (image.getSize() > 10 * 1024 * 1024) {
            throw new GlobalException(ErrorCode.IMAGE_TOO_LARGE);
        }
        if (!"image/png".equals(image.getContentType())) {
            throw new GlobalException(ErrorCode.INVALID_IMAGE);
        }
    }


}
