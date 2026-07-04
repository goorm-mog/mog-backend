package com.mog.project.meeting.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mog.project.domain.meeting.controller.MeetingRecordController;
import com.mog.project.domain.meeting.dto.request.MeetingRecordCreateRequest;
import com.mog.project.domain.meeting.dto.request.MeetingRecordUpdateRequest;
import com.mog.project.domain.meeting.dto.request.ParticipantRequest;
import com.mog.project.domain.meeting.dto.response.MeetingRecordListResponse;
import com.mog.project.domain.meeting.dto.response.MeetingRecordResponse;
import com.mog.project.domain.meeting.dto.response.ParticipantResponse;
import com.mog.project.domain.meeting.service.MeetingRecordService;
import com.mog.project.domain.meeting.service.OcrService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MeetingRecordController.class)
@WithMockUser
@ActiveProfiles("test")
class MeetingRecordControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean MeetingRecordService meetingRecordService;
    @MockBean OcrService ocrService;

    private MeetingRecordResponse sampleResponse() {
        return new MeetingRecordResponse(
                1L, 1, "강남", "메모", 10000, null,
                List.of(new ParticipantResponse(100L, "멤버100", 10000)),
                LocalDateTime.now()
        );
    }

    // ── GET /api/v1/rooms/{roomId}/records ──────────────────────────────────

    @Test
    void getRecords_200_반환() throws Exception {
        MeetingRecordListResponse response = new MeetingRecordListResponse(List.of(), List.of());
        when(meetingRecordService.getRecords(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/rooms/1/records"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.photos").isArray())
                .andExpect(jsonPath("$.data.records").isArray());
    }

    // ── POST /api/v1/rooms/{roomId}/records ─────────────────────────────────

    @Test
    void createRecord_201_반환() throws Exception {
        when(meetingRecordService.createRecord(eq(1L), any())).thenReturn(sampleResponse());

        MeetingRecordCreateRequest request = new MeetingRecordCreateRequest(
                "강남", null, null,
                List.of(new ParticipantRequest(100L, 10000))
        );

        mockMvc.perform(post("/api/v1/rooms/1/records")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.placeName").value("강남"))
                .andExpect(jsonPath("$.data.seq").value(1))
                .andExpect(jsonPath("$.data.totalCost").value(10000));
    }

    @Test
    void createRecord_placeName_없으면_400() throws Exception {
        String body = """
                {
                    "placeName": "",
                    "participants": [{"roomMemberId": 1, "amount": 1000}]
                }
                """;

        mockMvc.perform(post("/api/v1/rooms/1/records")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRecord_participants_빈_배열이면_400() throws Exception {
        String body = """
                {
                    "placeName": "강남",
                    "participants": []
                }
                """;

        mockMvc.perform(post("/api/v1/rooms/1/records")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRecord_participants_누락이면_400() throws Exception {
        String body = """
                {
                    "placeName": "강남"
                }
                """;

        mockMvc.perform(post("/api/v1/rooms/1/records")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ── PATCH /api/v1/rooms/{roomId}/records/{recordId} ─────────────────────

    @Test
    void updateRecord_200_반환() throws Exception {
        MeetingRecordResponse updated = new MeetingRecordResponse(
                1L, 1, "수정된 장소", null, 0, null, List.of(), LocalDateTime.now()
        );
        when(meetingRecordService.updateRecord(eq(1L), eq(1L), any())).thenReturn(updated);

        mockMvc.perform(patch("/api/v1/rooms/1/records/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new MeetingRecordUpdateRequest("수정된 장소", null, null, null)
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.placeName").value("수정된 장소"));
    }

    // ── DELETE /api/v1/rooms/{roomId}/records/{recordId} ────────────────────

    @Test
    void deleteRecord_200_반환() throws Exception {
        doNothing().when(meetingRecordService).deleteRecord(1L, 1L);

        mockMvc.perform(delete("/api/v1/rooms/1/records/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("만남 기록을 삭제했습니다."));
    }
}
