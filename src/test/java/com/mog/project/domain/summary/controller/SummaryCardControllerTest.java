package com.mog.project.domain.summary.controller;

import com.mog.project.domain.meeting.dto.response.MenuItemResponse;
import com.mog.project.domain.meeting.dto.response.PlaceResponse;
import com.mog.project.domain.summary.service.SummaryCardService;
import com.mog.project.domain.summary.dto.response.*;
import com.mog.project.global.exception.ErrorCode;
import com.mog.project.global.exception.GlobalException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SummaryCardController.class)
@WithMockUser
@ActiveProfiles("test")
class SummaryCardControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean SummaryCardService summaryCardService;

    private SummaryCardResponse sampleResponse() {
        List<SummaryParticipantResponse> participants = List.of(
                new SummaryParticipantResponse("김구름", 10000),
                new SummaryParticipantResponse("박구름", 8000)
        );
        List<SummaryRecordResponse> records = List.of(
                new SummaryRecordResponse(1, new PlaceResponse("합정 카페", null), "메모", 18000, participants, List.of())
        );
        List<SummaryMemberTotalResponse> memberTotals = List.of(
                new SummaryMemberTotalResponse("김구름", 25000),
                new SummaryMemberTotalResponse("박구름", 23000)
        );
        SummarySettlementResponse settlement = new SummarySettlementResponse(48000, memberTotals);

        return new SummaryCardResponse(
                1L,
                LocalDate.of(2026, 6, 14),
                null,
                2,
                List.of("김구름", "박구름"),
                List.of("https://s3.amazonaws.com/photo1.jpg"),
                records,
                settlement,
                null
        );
    }

    // ── GET /api/v1/rooms/{roomId}/summary ──────────────────────────────────

    @Test
    void getSummaryData_성공시_200_반환() throws Exception {
        when(summaryCardService.getSummaryData(eq(1L), any())).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/rooms/1/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("요약 카드 데이터를 조회했습니다."))
                .andExpect(jsonPath("$.data.roomId").value(1))
                .andExpect(jsonPath("$.data.totalMemberCount").value(2))
                .andExpect(jsonPath("$.data.members").isArray())
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.records[0].seq").value(1))
                .andExpect(jsonPath("$.data.settlement.totalCost").value(48000))
                .andExpect(jsonPath("$.data.cardImageUrl").isEmpty());
    }

    @Test
    void getSummaryData_방_없으면_404() throws Exception {
        when(summaryCardService.getSummaryData(eq(999L), any()))
                .thenThrow(new GlobalException(ErrorCode.ROOM_NOT_FOUND));

        mockMvc.perform(get("/api/v1/rooms/999/summary"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSummaryData_방멤버_아니면_403() throws Exception {
        when(summaryCardService.getSummaryData(eq(1L), any()))
                .thenThrow(new GlobalException(ErrorCode.FORBIDDEN));

        mockMvc.perform(get("/api/v1/rooms/1/summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getSummaryData_정산_미확정이면_409() throws Exception {
        when(summaryCardService.getSummaryData(eq(1L), any()))
                .thenThrow(new GlobalException(ErrorCode.SETTLEMENT_NOT_CONFIRMED));

        mockMvc.perform(get("/api/v1/rooms/1/summary"))
                .andExpect(status().isConflict());
    }

    // ── POST /api/v1/rooms/{roomId}/summary/card ────────────────────────────

    @Test
    void saveCardImage_성공시_200_반환() throws Exception {
        CardImageResponse response = new CardImageResponse("https://s3.amazonaws.com/cards/card.png");
        when(summaryCardService.saveCardImage(eq(1L), any(), any())).thenReturn(response);

        MockMultipartFile image = new MockMultipartFile(
                "image", "card.png", "image/png", "fake-image".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/rooms/1/summary/card")
                        .file(image)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("요약 카드 이미지가 저장되었습니다."))
                .andExpect(jsonPath("$.data.cardImageUrl").value("https://s3.amazonaws.com/cards/card.png"));
    }

    @Test
    void saveCardImage_빈_파일이면_400() throws Exception {
        when(summaryCardService.saveCardImage(eq(1L), any(), any()))
                .thenThrow(new GlobalException(ErrorCode.INVALID_IMAGE));

        MockMultipartFile emptyImage = new MockMultipartFile(
                "image", "", "image/png", new byte[0]
        );

        mockMvc.perform(multipart("/api/v1/rooms/1/summary/card")
                        .file(emptyImage)
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void saveCardImage_방_없으면_404() throws Exception {
        when(summaryCardService.saveCardImage(eq(999L), any(), any()))
                .thenThrow(new GlobalException(ErrorCode.ROOM_NOT_FOUND));

        MockMultipartFile image = new MockMultipartFile(
                "image", "card.png", "image/png", "fake".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/rooms/999/summary/card")
                        .file(image)
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void saveCardImage_정산_미확정이면_409() throws Exception {
        when(summaryCardService.saveCardImage(eq(1L), any(), any()))
                .thenThrow(new GlobalException(ErrorCode.SETTLEMENT_NOT_CONFIRMED));

        MockMultipartFile image = new MockMultipartFile(
                "image", "card.png", "image/png", "fake".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/rooms/1/summary/card")
                        .file(image)
                        .with(csrf()))
                .andExpect(status().isConflict());
    }
}
