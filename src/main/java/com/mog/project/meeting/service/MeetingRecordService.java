package com.mog.project.meeting.service;

import com.mog.project.global.exception.ErrorCode;
import com.mog.project.global.exception.GlobalException;
import com.mog.project.meeting.dto.request.MeetingRecordCreateRequest;
import com.mog.project.meeting.dto.request.MeetingRecordUpdateRequest;
import com.mog.project.meeting.dto.response.MeetingRecordListResponse;
import com.mog.project.meeting.dto.response.MeetingRecordResponse;
import com.mog.project.meeting.entity.MeetingMemberCost;
import com.mog.project.meeting.entity.MeetingRecord;

import com.mog.project.meeting.repository.MeetingMemberCostRepository;
import com.mog.project.meeting.repository.MeetingRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingRecordService {

    private final MeetingRecordRepository meetingRecordRepository;
    private final MeetingMemberCostRepository meetingMemberCostRepository;

    // 해당 방의 전체 차수 기록 + 사진 조회
    public MeetingRecordListResponse getRecords(Long roomId) {
        List<MeetingRecord> records = meetingRecordRepository.findByRoomId(roomId);

        List<MeetingMemberCost> allCosts = meetingMemberCostRepository.findByMeetingRecordIn(records);

        return MeetingRecordListResponse.from(List.of(), records, allCosts);
    }

    // 새 차수 추가
    @Transactional
    public MeetingRecordResponse createRecord(Long roomId, MeetingRecordCreateRequest request) {

        // 현재 방의 차수를 자동으로 + 1
        int nextSeq = meetingRecordRepository.findMaxSeqByRoomId(roomId) + 1;

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

        // dto로 형변환
        List<MeetingMemberCost> costs = request.participants().stream()
                .map(p -> MeetingMemberCost.builder()
                        .meetingRecord(record)
                        .roomMemberId(p.roomMemberId())
                        .amount(p.amount())
                        .build())
                .toList();
        meetingMemberCostRepository.saveAll(costs);

        return MeetingRecordResponse.from(record, costs);
    }

    // 특정 차수 수정
    @Transactional
    public MeetingRecordResponse updateRecord(Long roomId, Long recordId, MeetingRecordUpdateRequest request) {

        // 다른방 기록 수정 방지
        MeetingRecord record = meetingRecordRepository.findByIdAndRoomId(recordId, roomId)
                .orElseThrow(() -> new GlobalException(ErrorCode.RECORD_NOT_FOUND));

        record.update(
                request.placeName(),
                request.memo(),
                request.payer() != null ? request.payer().roomMemberId() : null,
                request.payer() != null ? request.payer().bankName() : null,
                request.payer() != null ? request.payer().accountNumber() : null
        );

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

        return MeetingRecordResponse.from(record, costs);
    }

    // 특정 차수 삭제
    @Transactional
    public void deleteRecord(Long roomId, Long recordId) {

        MeetingRecord record = meetingRecordRepository.findByIdAndRoomId(recordId, roomId)
                .orElseThrow(() -> new GlobalException(ErrorCode.RECORD_NOT_FOUND));

        int seq = record.getSeq();

        meetingRecordRepository.delete(record);

        meetingRecordRepository.decreaseSeqAfter(roomId, seq);
    }
}
