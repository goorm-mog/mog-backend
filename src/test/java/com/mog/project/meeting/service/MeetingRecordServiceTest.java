package com.mog.project.meeting.service;

import com.mog.project.domain.groups.entity.GroupMember;
import com.mog.project.domain.groups.repository.GroupMemberRepository;
import com.mog.project.domain.meeting.service.MeetingRecordService;
import com.mog.project.domain.meeting.service.RoomPhotoService;
import com.mog.project.domain.notification.service.NotificationService;
import com.mog.project.domain.room.entity.Room;
import com.mog.project.domain.room.repository.RoomRepository;
import com.mog.project.domain.groups.entity.Group;
import com.mog.project.domain.user.entity.User;
import com.mog.project.domain.user.repository.UserRepository;
import com.mog.project.global.exception.GlobalException;
import com.mog.project.domain.meeting.dto.request.MeetingRecordCreateRequest;
import com.mog.project.domain.meeting.dto.request.MeetingRecordUpdateRequest;
import com.mog.project.domain.meeting.dto.request.ParticipantRequest;
import com.mog.project.domain.meeting.dto.response.MeetingRecordListResponse;
import com.mog.project.domain.meeting.dto.response.MeetingRecordResponse;
import com.mog.project.domain.meeting.entity.MeetingRecord;
import com.mog.project.domain.meeting.repository.MeetingMemberCostRepository;
import com.mog.project.domain.meeting.repository.MeetingMenuItemRepository;
import com.mog.project.domain.meeting.repository.MeetingRecordRepository;
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
    @Mock MeetingMenuItemRepository meetingMenuItemRepository;
    @Mock RoomPhotoService roomPhotoService;
    @Mock RoomRepository roomRepository;
    @Mock GroupMemberRepository groupMemberRepository;
    @Mock UserRepository userRepository;
    @Mock NotificationService notificationService;
    @InjectMocks MeetingRecordService meetingRecordService;

    private MeetingRecord record;

    @BeforeEach
    void setUp() {
        record = MeetingRecord.builder()
                .roomId(1L).seq(1).placeName("강남").memo("첫 만남").build();
        ReflectionTestUtils.setField(record, "id", 1L);

        // buildNicknameMap() + checkMembership() + notifyRoomMembers() 호출 시 NPE 방지용 공통 mock 설정
        // lenient: 일부 테스트에서 호출되지 않는 stub에 대한 경고 방지
        Room mockRoom = mock(Room.class);
        Group mockGroup = mock(Group.class);
        User mockUser = mock(User.class);
        GroupMember mockGroupMember = mock(GroupMember.class);
        lenient().when(mockRoom.getGroup()).thenReturn(mockGroup);
        lenient().when(mockGroup.getGroupId()).thenReturn(1L);
        lenient().when(mockUser.getUserId()).thenReturn(1L);
        lenient().when(roomRepository.findById(1L)).thenReturn(Optional.of(mockRoom));
        lenient().when(userRepository.findByKakaoId(any())).thenReturn(Optional.of(mockUser));
        lenient().when(groupMemberRepository.findByGroupGroupIdAndUserUserId(anyLong(), anyLong()))
                .thenReturn(Optional.of(mockGroupMember));
        lenient().when(groupMemberRepository.findByGroupGroupId(1L)).thenReturn(List.of());
    }

    // ── getRecords ──────────────────────────────────────────────────────────

    @Test
    void getRecords_빈_방이면_빈_목록_반환() {
        when(meetingRecordRepository.findByRoomId(1L)).thenReturn(List.of());
        when(meetingMemberCostRepository.findByMeetingRecordIn(List.of())).thenReturn(List.of());
        when(meetingMenuItemRepository.findByMeetingRecordIn(List.of())).thenReturn(List.of());
        when(roomPhotoService.getPhotos(1L)).thenReturn(List.of());

        MeetingRecordListResponse response = meetingRecordService.getRecords(1L, "kakaoId");

        assertThat(response.records()).isEmpty();
        assertThat(response.photos()).isEmpty();
    }

    @Test
    void getRecords_기록이_있으면_placeName_포함하여_반환() {
        when(meetingRecordRepository.findByRoomId(1L)).thenReturn(List.of(record));
        when(meetingMemberCostRepository.findByMeetingRecordIn(List.of(record))).thenReturn(List.of());
        when(meetingMenuItemRepository.findByMeetingRecordIn(List.of(record))).thenReturn(List.of());
        when(roomPhotoService.getPhotos(1L)).thenReturn(List.of());

        MeetingRecordListResponse response = meetingRecordService.getRecords(1L, "kakaoId");

        assertThat(response.records()).hasSize(1);
        assertThat(response.records().get(0).placeName()).isEqualTo("강남");
    }

    // ── createRecord ────────────────────────────────────────────────────────

    @Test
    void createRecord_seq를_maxSeq_더하기_1로_자동_부여() {
        when(meetingRecordRepository.findMaxSeqByRoomId(1L)).thenReturn(2);
        when(meetingRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(meetingMemberCostRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(meetingMenuItemRepository.findByMeetingRecordIn(any())).thenReturn(List.of());

        MeetingRecordCreateRequest request = new MeetingRecordCreateRequest(
                "홍대", null, null, null,
                List.of(new ParticipantRequest(100L, 10000))
        );

        assertThat(meetingRecordService.createRecord(1L, "kakaoId", request).seq()).isEqualTo(3);
    }

    @Test
    void createRecord_첫_기록이면_seq가_1() {
        when(meetingRecordRepository.findMaxSeqByRoomId(1L)).thenReturn(0);
        when(meetingRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(meetingMemberCostRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(meetingMenuItemRepository.findByMeetingRecordIn(any())).thenReturn(List.of());

        MeetingRecordCreateRequest request = new MeetingRecordCreateRequest(
                "이태원", null, null, null,
                List.of(new ParticipantRequest(100L, 5000))
        );

        assertThat(meetingRecordService.createRecord(1L, "kakaoId", request).seq()).isEqualTo(1);
    }

    @Test
    void createRecord_참여자_전원의_비용_합계가_totalCost() {
        when(meetingRecordRepository.findMaxSeqByRoomId(1L)).thenReturn(0);
        when(meetingRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(meetingMemberCostRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(meetingMenuItemRepository.findByMeetingRecordIn(any())).thenReturn(List.of());

        MeetingRecordCreateRequest request = new MeetingRecordCreateRequest(
                "건대", null, null, null,
                List.of(
                        new ParticipantRequest(100L, 10000),
                        new ParticipantRequest(200L, 15000)
                )
        );

        MeetingRecordResponse response = meetingRecordService.createRecord(1L, "kakaoId", request);

        assertThat(response.totalCost()).isEqualTo(25000);
        assertThat(response.participants()).hasSize(2);
    }

    // ── updateRecord ────────────────────────────────────────────────────────

    @Test
    void updateRecord_존재하지_않는_기록이면_예외() {
        when(meetingRecordRepository.findByIdAndRoomId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                meetingRecordService.updateRecord(1L, 99L, "kakaoId",
                        new MeetingRecordUpdateRequest(null, null, null, null, null)))
                .isInstanceOf(GlobalException.class);
    }

    @Test
    void updateRecord_participants_생략하면_기존_참여자_유지() {
        when(meetingRecordRepository.findByIdAndRoomId(1L, 1L)).thenReturn(Optional.of(record));
        when(meetingMemberCostRepository.findByMeetingRecordIn(List.of(record))).thenReturn(List.of());
        when(meetingMenuItemRepository.findByMeetingRecordIn(any())).thenReturn(List.of());

        meetingRecordService.updateRecord(1L, 1L, "kakaoId",
                new MeetingRecordUpdateRequest(null, null, null, null, null));

        verify(meetingMemberCostRepository, never()).deleteByMeetingRecord(any());
    }

    @Test
    void updateRecord_participants_포함하면_기존_참여자_전체_교체() {
        when(meetingRecordRepository.findByIdAndRoomId(1L, 1L)).thenReturn(Optional.of(record));
        when(meetingMemberCostRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(meetingMenuItemRepository.findByMeetingRecordIn(any())).thenReturn(List.of());

        meetingRecordService.updateRecord(1L, 1L, "kakaoId",
                new MeetingRecordUpdateRequest(null, null, null, null,
                        List.of(new ParticipantRequest(200L, 20000))));

        verify(meetingMemberCostRepository).deleteByMeetingRecord(record);
        verify(meetingMemberCostRepository).saveAll(any());
    }

    // ── deleteRecord ────────────────────────────────────────────────────────

    @Test
    void deleteRecord_존재하지_않는_기록이면_예외() {
        when(meetingRecordRepository.findByIdAndRoomId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> meetingRecordService.deleteRecord(1L, 99L, "kakaoId"))
                .isInstanceOf(GlobalException.class);
    }

    @Test
    void deleteRecord_MeetingMemberCost를_먼저_삭제_후_MeetingRecord_삭제() {
        when(meetingRecordRepository.findByIdAndRoomId(1L, 1L)).thenReturn(Optional.of(record));

        meetingRecordService.deleteRecord(1L, 1L, "kakaoId");

        InOrder inOrder = inOrder(meetingMemberCostRepository, meetingMenuItemRepository, meetingRecordRepository);
        inOrder.verify(meetingMemberCostRepository).deleteByMeetingRecord(record);
        inOrder.verify(meetingMenuItemRepository).deleteByMeetingRecord(record);
        inOrder.verify(meetingRecordRepository).delete(record);
    }

    @Test
    void deleteRecord_삭제된_seq_이후_차수가_1씩_감소() {
        MeetingRecord seq2Record = MeetingRecord.builder()
                .roomId(1L).seq(2).placeName("홍대").build();
        ReflectionTestUtils.setField(seq2Record, "id", 2L);
        when(meetingRecordRepository.findByIdAndRoomId(2L, 1L)).thenReturn(Optional.of(seq2Record));

        meetingRecordService.deleteRecord(1L, 2L, "kakaoId");

        verify(meetingRecordRepository).decreaseSeqAfter(1L, 2);
    }
}
