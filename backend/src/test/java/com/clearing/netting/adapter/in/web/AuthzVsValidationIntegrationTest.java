package com.clearing.netting.adapter.in.web;

import com.clearing.netting.application.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression: authorization failures (403) must be distinguishable from
 * validation failures (400). Authz used to be remapped to 400, so a viewer
 * clicking a write action saw the same "输入不合法" copy as a genuinely
 * malformed form.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthzVsValidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private ObjectMapper objectMapper;

    private String bearer(String role) {
        return "Bearer " + tokenService.issueToken("tester-" + role, role);
    }

    @Test
    void viewerWriteObligationGets403ForbiddenEvenWhenBodyIsInvalid() throws Exception {
        // Deliberately missing fields: under the old behavior validation would
        // (wrongly) dominate and the viewer saw a 400 validation error.
        String body = "{}";

        mockMvc.perform(post("/api/obligations")
                        .header("Authorization", bearer("VIEWER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void viewerExecuteNettingGets403Forbidden() throws Exception {
        mockMvc.perform(post("/api/netting-runs")
                        .header("Authorization", bearer("VIEWER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void operatorWithMissingFieldsGets400ValidationError() throws Exception {
        mockMvc.perform(post("/api/obligations")
                        .header("Authorization", bearer("OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void operatorExecuteNettingWithMissingFieldsGets400ValidationError() throws Exception {
        mockMvc.perform(post("/api/netting-runs")
                        .header("Authorization", bearer("OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void noTokenOnWriteGets401Unauthorized() throws Exception {
        mockMvc.perform(post("/api/obligations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void forbiddenAndValidationResponsesAreStructurallyDistinct() throws Exception {
        int authzStatus = mockMvc.perform(post("/api/obligations")
                        .header("Authorization", bearer("VIEWER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn().getResponse().getStatus();

        int validationStatus = mockMvc.perform(post("/api/obligations")
                        .header("Authorization", bearer("OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn().getResponse().getStatus();

        assertThat(authzStatus).isEqualTo(403);
        assertThat(validationStatus).isEqualTo(400);
        assertThat(authzStatus).isNotEqualTo(validationStatus);

        String authzBody = mockMvc.perform(post("/api/netting-runs")
                        .header("Authorization", bearer("VIEWER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn().getResponse().getContentAsString();
        String validationBody = mockMvc.perform(post("/api/netting-runs")
                        .header("Authorization", bearer("OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(authzBody).get("code").asText())
                .isEqualTo("FORBIDDEN");
        assertThat(objectMapper.readTree(validationBody).get("code").asText())
                .isEqualTo("VALIDATION_ERROR");
    }
}
