package com.mog.project.domain.settlement.controller;

import com.mog.project.domain.settlement.dto.response.MemberSettlementResponse;
import com.mog.project.domain.settlement.dto.response.SettlementResponse;
import com.mog.project.domain.settlement.service.SettlementService;
import com.mog.project.global.exception.ErrorCode;
import com.mog.project.global.exception.GlobalException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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

@WebMvcTest(SettlementController.class)
@WithMockUser   // SecurityContext에 가짜 인증 정보 주입 (JWT 필터 우회)
@ActiveProfiles("test")
class SettlementControllerTest {

    @Autowired MockMvc mockMvc;

    // 실제 Service 대신 Mock으로 대체 → 컨트롤러 레이어만 테스트
    @MockBean SettlementService settlementService;

    // 테스트에서 반복 사용할 샘플 응답 객체
    private SettlementResponse sampleResponse() {
        List<MemberSettlementResponse> members = List.of(
                new MemberSettlementResponse(101L, "김구름", 25000, List.of()),
                new MemberSettlementResponse(102L, "박구름", 23000, List.of())
        );
        return new SettlementResponse(1L, 48000, false, null, members, LocalDateTime.now());
    }

    private SettlementResponse confirmedResponse() {
        List<MemberSettlementResponse> members = List.of(
                new MemberSettlementResponse(101L, "김구름", 25000, List.of())
        );
        return new SettlementResponse(1L, 25000, true, LocalDateTime.now(), members, LocalDateTime.now());
    }

    // ── POST /api/v1/rooms/{roomId}/settlement ──────────────────────────────

    @Test
    void calculateSettlement_성공시_201_반환() throws Exception {
        // @AuthenticationPrincipal은 @WithMockUser 환경에서 null로 주입됨
        // → 서비스는 any()로 매칭
        when(settlementService.calculateAndCreate(eq(1L), any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/rooms/1/settlement")
                        .with(csrf()))   // CSRF 토큰 포함 (POST/PATCH/DELETE 필수)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("정산이 계산되었습니다."))
                .andExpect(jsonPath("$.data.settlementId").value(1))
                .andExpect(jsonPath("$.data.totalCost").value(48000))
                .andExpect(jsonPath("$.data.isConfirmed").value(false))
                .andExpect(jsonPath("$.data.memberSettlements").isArray())
                .andExpect(jsonPath("$.data.memberSettlements[0].nickname").value("김구름"));
    }

    @Test
    void calculateSettlement_차수기록_없으면_400() throws Exception {
        // 서비스에서 NO_RECORDS 예외를 던지는 상황 시뮬레이션
        when(settlementService.calculateAndCreate(eq(1L), any()))
                .thenThrow(new GlobalException(ErrorCode.NO_RECORDS));

        mockMvc.perform(post("/api/v1/rooms/1/settlement")
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void calculateSettlement_존재하지_않는_방이면_404() throws Exception {
        when(settlementService.calculateAndCreate(eq(999L), any()))
                .thenThrow(new GlobalException(ErrorCode.ROOM_NOT_FOUND));

        mockMvc.perform(post("/api/v1/rooms/999/settlement")
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    // ── GET /api/v1/rooms/{roomId}/settlement ───────────────────────────────

    @Test
    void getSettlement_성공시_200_반환() throws Exception {
        when(settlementService.getSettlement(eq(1L), any())).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/rooms/1/settlement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("정산을 조회했습니다."))
                .andExpect(jsonPath("$.data.settlementId").value(1))
                .andExpect(jsonPath("$.data.totalCost").value(48000))
                .andExpect(jsonPath("$.data.memberSettlements").isArray());
    }

    @Test
    void getSettlement_정산_없으면_404() throws Exception {
        when(settlementService.getSettlement(eq(1L), any()))
                .thenThrow(new GlobalException(ErrorCode.SETTLEMENT_NOT_FOUND));

        mockMvc.perform(get("/api/v1/rooms/1/settlement"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSettlement_존재하지_않는_방이면_404() throws Exception {
        when(settlementService.getSettlement(eq(999L), any()))
                .thenThrow(new GlobalException(ErrorCode.ROOM_NOT_FOUND));

        mockMvc.perform(get("/api/v1/rooms/999/settlement"))
                .andExpect(status().isNotFound());
    }

    // ── PATCH /api/v1/rooms/{roomId}/settlement/confirm ─────────────────────

    @Test
    void confirmSettlement_성공시_200과_isConfirmed_true_반환() throws Exception {
        when(settlementService.confirmSettlement(eq(1L), any())).thenReturn(confirmedResponse());

        mockMvc.perform(patch("/api/v1/rooms/1/settlement/confirm")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("정산이 확정되었습니다."))
                .andExpect(jsonPath("$.data.isConfirmed").value(true))
                .andExpect(jsonPath("$.data.confirmedAt").isNotEmpty());
    }

    @Test
    void confirmSettlement_이미_확정된_경우_400() throws Exception {
        when(settlementService.confirmSettlement(eq(1L), any()))
                .thenThrow(new GlobalException(ErrorCode.ALREADY_CONFIRMED));

        mockMvc.perform(patch("/api/v1/rooms/1/settlement/confirm")
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void confirmSettlement_방장이_아니면_403() throws Exception {
        when(settlementService.confirmSettlement(eq(1L), any()))
                .thenThrow(new GlobalException(ErrorCode.NOT_HOST));

        mockMvc.perform(patch("/api/v1/rooms/1/settlement/confirm")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void confirmSettlement_정산_없으면_404() throws Exception {
        when(settlementService.confirmSettlement(eq(1L), any()))
                .thenThrow(new GlobalException(ErrorCode.SETTLEMENT_NOT_FOUND));

        mockMvc.perform(patch("/api/v1/rooms/1/settlement/confirm")
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }
}
