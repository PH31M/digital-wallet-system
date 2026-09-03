package com.digitalwallet.api.controller;

import com.digitalwallet.api.dto.response.AdminUserResponse;
import com.digitalwallet.config.SecurityConfig;
import com.digitalwallet.domain.entity.User;
import com.digitalwallet.domain.enums.UserRole;
import com.digitalwallet.security.CustomUserDetailsService;
import com.digitalwallet.security.CustomUserPrincipal;
import com.digitalwallet.security.SecurityErrorResponseWriter;
import com.digitalwallet.security.jwt.JwtTokenProvider;
import com.digitalwallet.security.jwt.TokenBlacklistService;
import com.digitalwallet.service.AdminUserService;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller test cho DWS-209: xác nhận @PreAuthorize("hasRole('ADMIN')") ở class-level
 * chặn đúng theo role, và các endpoint trả đúng format ApiResponse.
 *
 * Dùng @Import(SecurityConfig.class) vì @WebMvcTest không tự nạp @Configuration
 * tuỳ biến — cần nạp thủ công để @PreAuthorize thực sự được thi hành trong test.
 * SecurityErrorResponseWriter dùng bean thật (không mock) vì nó chịu trách nhiệm
 * set HTTP status cho response lỗi 401/403 — mock nó sẽ làm mất status thật.
 */
@WebMvcTest(AdminUserController.class)
@Import({SecurityConfig.class, SecurityErrorResponseWriter.class})
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminUserService adminUserService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void listUsers_asAdmin_returns200() throws Exception {
        when(adminUserService.listUsers(any(), any())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    void listUsers_asUser_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listUsers_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listAuditLogs_asAdmin_returns200() throws Exception {
        when(adminUserService.listAuditLogs(any(), any(), any(), any(), any(), any())).thenReturn(Page.empty());

        mockMvc.perform(get("/api/admin/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void updateRole_asAdmin_returns200AndCallsService() throws Exception {
        User admin = user("admin@example.com", UserRole.ADMIN);
        UUID targetId = UUID.randomUUID();
        when(adminUserService.updateRole(any(User.class), eq(targetId), eq(UserRole.ADMIN), any(), any()))
                .thenReturn(new AdminUserResponse(user("target@example.com", UserRole.ADMIN)));

        mockMvc.perform(patch("/api/admin/users/{userId}/role", targetId)
                        .with(authentication(adminAuthentication(admin)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(adminUserService).updateRole(eq(admin), eq(targetId), eq(UserRole.ADMIN), any(), any());
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateRole_asUser_returns403() throws Exception {
        mockMvc.perform(patch("/api/admin/users/{userId}/role", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());
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
