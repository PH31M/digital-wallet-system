package com.digitalwallet.api.controller;

import com.digitalwallet.api.dto.response.AdminDashboardOverviewResponse;
import com.digitalwallet.api.dto.response.AdminDashboardVolumeResponse;
import com.digitalwallet.api.dto.response.DailyVolumePoint;
import com.digitalwallet.config.SecurityConfig;
import com.digitalwallet.security.CustomUserDetailsService;
import com.digitalwallet.security.SecurityErrorResponseWriter;
import com.digitalwallet.security.jwt.JwtTokenProvider;
import com.digitalwallet.security.jwt.TokenBlacklistService;
import com.digitalwallet.service.AdminDashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminDashboardController.class)
@Import({SecurityConfig.class, SecurityErrorResponseWriter.class})
class AdminDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminDashboardService dashboardService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void overview_asAdmin_returns200() throws Exception {
        when(dashboardService.getOverview()).thenReturn(overview());

        mockMvc.perform(get("/api/admin/dashboard/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalUsers").value(10));
    }

    @Test
    @WithMockUser(roles = "USER")
    void overview_asUser_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/overview"))
                .andExpect(status().isForbidden());
    }

    @Test
    void overview_withoutAuthentication_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/overview"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void volume_withValidDays_returns200() throws Exception {
        when(dashboardService.getDailyVolume(7)).thenReturn(volume());

        mockMvc.perform(get("/api/admin/dashboard/volume").param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.days").value(7))
                .andExpect(jsonPath("$.data.series[0].transactionCount").value(3));

        verify(dashboardService).getDailyVolume(7);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void volume_withDaysOutsideRange_returns400() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/volume").param("days", "91"))
                .andExpect(status().isBadRequest());
    }

    private AdminDashboardOverviewResponse overview() {
        return new AdminDashboardOverviewResponse(10, 9, 10, BigDecimal.TEN,
                Map.of("COMPLETED", 1L), 0, 0.0, Instant.now());
    }

    private AdminDashboardVolumeResponse volume() {
        return new AdminDashboardVolumeResponse(7,
                List.of(new DailyVolumePoint(LocalDate.of(2026, 8, 19), BigDecimal.TEN, 3)), Instant.now());
    }
}