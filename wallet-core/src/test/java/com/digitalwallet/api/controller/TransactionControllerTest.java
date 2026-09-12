package com.digitalwallet.api.controller;

import com.digitalwallet.api.dto.response.TransactionHistoryItemResponse;
import com.digitalwallet.api.dto.response.TransactionResponse;
import com.digitalwallet.config.SecurityConfig;
import com.digitalwallet.config.WebMvcConfig;
import com.digitalwallet.domain.entity.User;
import com.digitalwallet.domain.enums.UserRole;
import com.digitalwallet.security.CustomUserDetailsService;
import com.digitalwallet.security.CustomUserPrincipal;
import com.digitalwallet.security.CurrentUserArgumentResolver;
import com.digitalwallet.security.SecurityErrorResponseWriter;
import com.digitalwallet.security.jwt.JwtTokenProvider;
import com.digitalwallet.security.jwt.TokenBlacklistService;
import com.digitalwallet.service.TransactionService;
import com.digitalwallet.service.WalletService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
@Import({SecurityConfig.class, SecurityErrorResponseWriter.class, WebMvcConfig.class,
        CurrentUserArgumentResolver.class})
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private WalletService walletService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @Test
    void history_asAuthenticatedUserReturnsOnlyServiceOwnedPage() throws Exception {
        User user = user("alice@example.com");
        when(transactionService.getHistory(eq(user.getId()), any(), any())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/v1/transactions/history")
                        .param("walletId", UUID.randomUUID().toString())
                        .with(authentication(userAuthentication(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isEmpty());

        verify(transactionService).getHistory(eq(user.getId()), any(), any());
    }

    @Test
    void history_withoutAuthenticationReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/transactions/history"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void history_passesFilterAndPaginationToService() throws Exception {
        User user = user("alice@example.com");
        UUID transactionId = UUID.randomUUID();
        UUID walletId = UUID.randomUUID();
        TransactionHistoryItemResponse item = new TransactionHistoryItemResponse(
                UUID.randomUUID(), transactionId, walletId, "DEPOSIT", "CREDIT",
                new BigDecimal("25.00"), null, "COMPLETED", "TX-001",
                Instant.parse("2026-08-19T10:00:00Z"));
        when(transactionService.getHistory(eq(user.getId()), any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(item),
                        org.springframework.data.domain.PageRequest.of(0, 5), 1));

        mockMvc.perform(get("/api/transactions/history")
                        .param("type", "DEPOSIT")
                        .param("status", "COMPLETED")
                        .param("minAmount", "10.00")
                        .param("maxAmount", "100.00")
                        .param("page", "0")
                        .param("size", "5")
                        .param("sort", "createdAt,desc")
                        .with(authentication(userAuthentication(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].type").value("DEPOSIT"))
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.totalElements").value(1));

        verify(transactionService).getHistory(eq(user.getId()), any(), any());
    }

    @Test
    void confirmOtp_asAuthenticatedUser_returns200AndCallsServiceWithCurrentUser() throws Exception {
        User user = user("alice@example.com");
        UUID transactionId = UUID.randomUUID();
        TransactionResponse response = new TransactionResponse(transactionId, "TX-001", UUID.randomUUID(),
                UUID.randomUUID(), "TRANSFER", new BigDecimal("6000000.00"), "COMPLETED",
                Instant.now(), Instant.now(), null);
        when(walletService.confirmOtp(eq(user), eq(transactionId), eq("123456"), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/transactions/{transactionId}/confirm-otp", transactionId)
                        .with(authentication(userAuthentication(user)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"otp\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        verify(walletService).confirmOtp(eq(user), eq(transactionId), eq("123456"), any());
    }

    @Test
    void confirmOtp_blankOtp_returns400WithoutCallingService() throws Exception {
        User user = user("alice@example.com");

        mockMvc.perform(post("/api/v1/transactions/{transactionId}/confirm-otp", UUID.randomUUID())
                        .with(authentication(userAuthentication(user)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"otp\":\"\"}"))
                .andExpect(status().isBadRequest());

        verify(walletService, never()).confirmOtp(any(), any(), any(), any());
    }

    @Test
    void confirmOtp_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/transactions/{transactionId}/confirm-otp", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"otp\":\"123456\"}"))
                .andExpect(status().isUnauthorized());
    }

    private Authentication userAuthentication(User user) {
        CustomUserPrincipal principal = new CustomUserPrincipal(user);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    private User user(String email) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setFullName("Test User");
        user.setPasswordHash("hash");
        user.setRole(UserRole.USER);
        user.setIsActive(true);
        return user;
    }
}