package com.mog.project.meeting.service;

import com.mog.project.global.exception.GlobalException;
import com.mog.project.meeting.dto.request.MeetingRecordCreateRequest;
import com.mog.project.meeting.dto.request.MeetingRecordUpdateRequest;
import com.mog.project.meeting.dto.request.ParticipantRequest;
import com.mog.project.meeting.dto.response.MeetingRecordListResponse;
import com.mog.project.meeting.dto.response.MeetingRecordResponse;
import com.mog.project.meeting.entity.MeetingMemberCost;
import com.mog.project.meeting.entity.MeetingRecord;
import com.mog.project.meeting.repository.MeetingMemberCostRepository;
import com.mog.project.meeting.repository.MeetingRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingRecordServiceTest {

    @Mock MeetingRecordRepository meetingRecordRepository;
    @Mock MeetingMemberCostRepository meetingMemberCostRepository;
    @Mock RoomPhotoService roomPhotoService;
    @InjectMocks MeetingRecordService meetingRecordService;

    private MeetingRecord record;

    @BeforeEach
    void setUp() {
        record = MeetingRecord.builder()
                .roomId(1L).seq(1).placeName("강남").memo("첫 만남").build();
        ReflectionTestUtils.setField(record, "id", 1L);
    }

    // ── getRecords ──────────────────────────────────────────────────────────

    @Test
    void getRecords_빈_방이면_빈_목록_반환() {
        when(meetingRecordRepository.findByRoomId(1L)).thenReturn(List.of());
        when(meetingMemberCostRepository.findByMeetingRecordIn(List.of())).thenReturn(List.of());
        when(roomPhotoService.getPhotos(1L)).thenReturn(List.of());

        MeetingRecordListResponse response = meetingRecordService.getRecords(1L);

        assertThat(response.records()).isEmpty();
        assertThat(response.photos()).isEmpty();
    }

    @Test
    void getRecords_기록이_있으면_placeName_포함하여_반환() {
        when(meetingRecordRepository.findByRoomId(1L)).thenReturn(List.of(record));
        when(meetingMemberCostRepository.findByMeetingRecordIn(List.of(record))).thenReturn(List.of());
        when(roomPhotoService.getPhotos(1L)).thenReturn(List.of());

        MeetingRecordListResponse response = meetingRecordService.getRecords(1L);

        assertThat(response.records()).hasSize(1);
        assertThat(response.records().get(0).placeName()).isEqualTo("강남");
    }

    // ── createRecord ────────────────────────────────────────────────────────

    @Test
    void createRecord_seq를_maxSeq_더하기_1로_자동_부여() {
        when(meetingRecordRepository.findMaxSeqByRoomId(1L)).thenReturn(2);
        when(meetingRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(meetingMemberCostRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        MeetingRecordCreateRequest request = new MeetingRecordCreateRequest(
                "홍대", null, null,
                List.of(new ParticipantRequest(100L, 10000))
        );

        assertThat(meetingRecordService.createRecord(1L, request).seq()).isEqualTo(3);
    }

    @Test
    void createRecord_첫_기록이면_seq가_1() {
        when(meetingRecordRepository.findMaxSeqByRoomId(1L)).thenReturn(0);
        when(meetingRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(meetingMemberCostRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        MeetingRecordCreateRequest request = new MeetingRecordCreateRequest(
                "이태원", null, null,
                List.of(new ParticipantRequest(100L, 5000))
        );

        assertThat(meetingRecordService.createRecord(1L, request).seq()).isEqualTo(1);
    }

    @Test
    void createRecord_참여자_전원의_비용_합계가_totalCost() {
        when(meetingRecordRepository.findMaxSeqByRoomId(1L)).thenReturn(0);
        when(meetingRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(meetingMemberCostRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        MeetingRecordCreateRequest request = new MeetingRecordCreateRequest(
                "건대", null, null,
                List.of(
                        new ParticipantRequest(100L, 10000),
                        new ParticipantRequest(200L, 15000)
                )
        );

        MeetingRecordResponse response = meetingRecordService.createRecord(1L, request);

        assertThat(response.totalCost()).isEqualTo(25000);
        assertThat(response.participants()).hasSize(2);
    }

    // ── updateRecord ────────────────────────────────────────────────────────

    @Test
    void updateRecord_존재하지_않는_기록이면_예외() {
        when(meetingRecordRepository.findByIdAndRoomId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                meetingRecordService.updateRecord(1L, 99L,
                        new MeetingRecordUpdateRequest(null, null, null, null)))
                .isInstanceOf(GlobalException.class);
    }

    @Test
    void updateRecord_participants_생략하면_기존_참여자_유지() {
        when(meetingRecordRepository.findByIdAndRoomId(1L, 1L)).thenReturn(Optional.of(record));
        when(meetingMemberCostRepository.findByMeetingRecordIn(List.of(record))).thenReturn(List.of());

        meetingRecordService.updateRecord(1L, 1L,
                new MeetingRecordUpdateRequest(null, null, null, null));

        verify(meetingMemberCostRepository, never()).deleteByMeetingRecord(any());
    }

    @Test
    void updateRecord_participants_포함하면_기존_참여자_전체_교체() {
        when(meetingRecordRepository.findByIdAndRoomId(1L, 1L)).thenReturn(Optional.of(record));
        when(meetingMemberCostRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        meetingRecordService.updateRecord(1L, 1L,
                new MeetingRecordUpdateRequest(null, null, null,
                        List.of(new ParticipantRequest(200L, 20000))));

        verify(meetingMemberCostRepository).deleteByMeetingRecord(record);
        verify(meetingMemberCostRepository).saveAll(any());
    }

    // ── deleteRecord ────────────────────────────────────────────────────────

    @Test
    void deleteRecord_존재하지_않는_기록이면_예외() {
        when(meetingRecordRepository.findByIdAndRoomId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> meetingRecordService.deleteRecord(1L, 99L))
                .isInstanceOf(GlobalException.class);
    }

    @Test
    void deleteRecord_MeetingMemberCost를_먼저_삭제_후_MeetingRecord_삭제() {
        when(meetingRecordRepository.findByIdAndRoomId(1L, 1L)).thenReturn(Optional.of(record));

        meetingRecordService.deleteRecord(1L, 1L);

        InOrder inOrder = inOrder(meetingMemberCostRepository, meetingRecordRepository);
        inOrder.verify(meetingMemberCostRepository).deleteByMeetingRecord(record);
        inOrder.verify(meetingRecordRepository).delete(record);
    }

    @Test
    void deleteRecord_삭제된_seq_이후_차수가_1씩_감소() {
        MeetingRecord seq2Record = MeetingRecord.builder()
                .roomId(1L).seq(2).placeName("홍대").build();
        ReflectionTestUtils.setField(seq2Record, "id", 2L);
        when(meetingRecordRepository.findByIdAndRoomId(2L, 1L)).thenReturn(Optional.of(seq2Record));

        meetingRecordService.deleteRecord(1L, 2L);

        verify(meetingRecordRepository).decreaseSeqAfter(1L, 2);
    }
}
