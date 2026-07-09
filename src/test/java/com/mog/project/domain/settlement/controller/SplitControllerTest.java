package com.mog.project.domain.settlement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mog.project.domain.settlement.dto.request.SplitRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SplitController.class)
@WithMockUser
@ActiveProfiles("test")
class SplitControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    // ── POST /api/v1/settlement/split ───────────────────────────────────────

    @Test
    void split_딱_나누어_떨어지면_전원_동일_금액() throws Exception {
        SplitRequest request = new SplitRequest(30000, List.of("김구름", "박구름", "이구름"));

        mockMvc.perform(post("/api/v1/settlement/split")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("금액 분배가 완료되었습니다."))
                .andExpect(jsonPath("$.data.totalAmount").value(30000))
                .andExpect(jsonPath("$.data.memberCount").value(3))
                .andExpect(jsonPath("$.data.splits").isArray())
                .andExpect(jsonPath("$.data.splits.length()").value(3));
    }

    @Test
    void split_나머지_발생시_분배_금액_합이_totalAmount와_일치() throws Exception {
        // 10000 / 3 = 3333 * 2명 + 3334 * 1명 = 10000
        SplitRequest request = new SplitRequest(10000, List.of("김구름", "박구름", "이구름"));

        mockMvc.perform(post("/api/v1/settlement/split")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(10000))
                .andExpect(jsonPath("$.data.memberCount").value(3))
                .andExpect(jsonPath("$.data.splits.length()").value(3));
    }

    @Test
    void split_1명이면_전액_본인_부담() throws Exception {
        SplitRequest request = new SplitRequest(15000, List.of("김구름"));

        mockMvc.perform(post("/api/v1/settlement/split")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.splits[0].amount").value(15000));
    }

    @Test
    void split_totalAmount_없으면_400() throws Exception {
        String body = """
                {
                    "members": ["김구름", "박구름"]
                }
                """;

        mockMvc.perform(post("/api/v1/settlement/split")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void split_totalAmount_0이면_400() throws Exception {
        SplitRequest request = new SplitRequest(0, List.of("김구름", "박구름"));

        mockMvc.perform(post("/api/v1/settlement/split")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void split_members_빈_배열이면_400() throws Exception {
        SplitRequest request = new SplitRequest(10000, List.of());

        mockMvc.perform(post("/api/v1/settlement/split")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void split_members_누락이면_400() throws Exception {
        String body = """
                {
                    "totalAmount": 10000
                }
                """;

        mockMvc.perform(post("/api/v1/settlement/split")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
