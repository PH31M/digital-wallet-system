package com.digitalwallet.api.controller;

import com.digitalwallet.api.dto.response.FraudReviewResponse;
import com.digitalwallet.config.SecurityConfig;
import com.digitalwallet.domain.entity.User;
import com.digitalwallet.domain.enums.FraudReviewAction;
import com.digitalwallet.domain.enums.FraudReviewStatus;
import com.digitalwallet.domain.enums.UserRole;
import com.digitalwallet.security.CustomUserDetailsService;
import com.digitalwallet.security.CustomUserPrincipal;
import com.digitalwallet.security.SecurityErrorResponseWriter;
import com.digitalwallet.security.jwt.JwtTokenProvider;
import com.digitalwallet.security.jwt.TokenBlacklistService;
import com.digitalwallet.service.AdminFraudReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller test cho DWS-230: xác nhận @PreAuthorize("hasRole('ADMIN')") ở class-level
 * chặn đúng theo role, và endpoint review() resolve đúng @CurrentUser (chỉ hoạt động với
 * CustomUserPrincipal, không phải UserDetails generic của @WithMockUser).
 */
@WebMvcTest(AdminFraudReviewController.class)
@Import({SecurityConfig.class, SecurityErrorResponseWriter.class})
class AdminFraudReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminFraudReviewService adminFraudReviewService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void list_asAdmin_returns200() throws Exception {
        when(adminFraudReviewService.list(any(), any())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/admin/fraud-assessments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    void list_asUser_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/fraud-assessments"))
                .andExpect(status().isForbidden());
    }

    @Test
    void list_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/fraud-assessments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void list_withoutReviewStatusParam_defaultsToPendingReview() throws Exception {
        when(adminFraudReviewService.list(any(), any())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/admin/fraud-assessments"))
                .andExpect(status().isOk());

        verify(adminFraudReviewService).list(eq(FraudReviewStatus.PENDING_REVIEW), any());
    }

    @Test
    void review_asAdmin_returns200AndCallsServiceWithCurrentUser() throws Exception {
        User admin = user("admin@example.com", UserRole.ADMIN);
        UUID assessmentId = UUID.randomUUID();
        when(adminFraudReviewService.review(any(User.class), eq(assessmentId), eq(FraudReviewAction.APPROVED),
                any(), any())).thenReturn(sampleResponse(assessmentId));

        mockMvc.perform(post("/api/admin/fraud-assessments/{assessmentId}/review", assessmentId)
                        .with(authentication(adminAuthentication(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"APPROVED\",\"note\":\"looks fine\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(adminFraudReviewService).review(eq(admin), eq(assessmentId), eq(FraudReviewAction.APPROVED),
                eq("looks fine"), any());
    }

    @Test
    @WithMockUser(roles = "USER")
    void review_asUser_returns403() throws Exception {
        mockMvc.perform(post("/api/admin/fraud-assessments/{assessmentId}/review", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"APPROVED\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void review_missingAction_returns400() throws Exception {
        User admin = user("admin@example.com", UserRole.ADMIN);

        mockMvc.perform(post("/api/admin/fraud-assessments/{assessmentId}/review", UUID.randomUUID())
                        .with(authentication(adminAuthentication(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"missing action\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void review_noteContainsHtml_returns400() throws Exception {
        User admin = user("admin@example.com", UserRole.ADMIN);

        mockMvc.perform(post("/api/admin/fraud-assessments/{assessmentId}/review", UUID.randomUUID())
                        .with(authentication(adminAuthentication(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"APPROVED\",\"note\":\"<script>alert(1)</script>\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.field").value("note"));
    }

    private FraudReviewResponse sampleResponse(UUID assessmentId) {
        return new FraudReviewResponse(assessmentId, null, "ALLOW", BigDecimal.ZERO, null,
                FraudReviewStatus.REVIEWED.name(), FraudReviewAction.APPROVED.name(), "looks fine", Instant.now(),
                UUID.randomUUID());
    }

    private Authentication adminAuthentication(User admin) {
        CustomUserPrincipal principal = new CustomUserPrincipal(admin);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    private User user(String email, UserRole role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setFullName("Test User");
        user.setRole(role);
        user.setIsActive(true);
        return user;
    }
}
