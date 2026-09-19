package com.clearing.netting.adapter.in.web;

import com.clearing.netting.application.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression: authorization failures (403) must stay distinguishable from
 * validation failures (400). A viewer hitting a write endpoint is forbidden;
 * an operator missing a required field is a validation error.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthzVsValidationWebTest {

    private static final String VALID_RUN_BODY = "{\"settleDate\":\"2026-09-19\",\"currency\":\"USD\"}";
    private static final String RUN_BODY_MISSING_CURRENCY = "{\"settleDate\":\"2026-09-19\"}";
    private static final String VALID_OBLIGATION_BODY =
            "{\"payerMemberId\":\"A\",\"payeeMemberId\":\"B\",\"currency\":\"USD\","
                    + "\"amount\":100,\"tradeDate\":\"2026-09-18\",\"settleDate\":\"2026-09-19\"}";
    private static final String OBLIGATION_BODY_MISSING_PAYER =
            "{\"payeeMemberId\":\"B\",\"currency\":\"USD\","
                    + "\"amount\":100,\"tradeDate\":\"2026-09-18\",\"settleDate\":\"2026-09-19\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    private String bearer(String username, String role) {
        return "Bearer " + tokenService.issueToken(username, role);
    }

    @Test
    void viewerExecutingNettingRunIsForbidden() throws Exception {
        mockMvc.perform(post("/api/netting-runs")
                        .header("Authorization", bearer("viewer", "VIEWER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_RUN_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void operatorExecutingNettingRunWithoutCurrencyIsValidationError() throws Exception {
        mockMvc.perform(post("/api/netting-runs")
                        .header("Authorization", bearer("operator", "OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RUN_BODY_MISSING_CURRENCY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void viewerCreatingObligationIsForbidden() throws Exception {
        mockMvc.perform(post("/api/obligations")
                        .header("Authorization", bearer("viewer", "VIEWER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_OBLIGATION_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void operatorCreatingObligationWithoutPayerIsValidationError() throws Exception {
        mockMvc.perform(post("/api/obligations")
                        .header("Authorization", bearer("operator", "OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OBLIGATION_BODY_MISSING_PAYER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void forbiddenAndValidationFailuresHaveDifferentStatuses() throws Exception {
        MvcResult forbidden = mockMvc.perform(post("/api/netting-runs")
                        .header("Authorization", bearer("viewer", "VIEWER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_RUN_BODY))
                .andReturn();
        MvcResult invalid = mockMvc.perform(post("/api/netting-runs")
                        .header("Authorization", bearer("operator", "OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RUN_BODY_MISSING_CURRENCY))
                .andReturn();

        assertThat(forbidden.getResponse().getStatus()).isEqualTo(403);
        assertThat(invalid.getResponse().getStatus()).isEqualTo(400);
        assertThat(forbidden.getResponse().getStatus())
                .as("authz failure must not masquerade as a validation failure")
                .isNotEqualTo(invalid.getResponse().getStatus());
    }
}
