package com.digitalwallet.api.controller;

import com.digitalwallet.config.SecurityConfig;
import com.digitalwallet.config.WebMvcConfig;
import com.digitalwallet.domain.entity.User;
import com.digitalwallet.domain.enums.UserRole;
import com.digitalwallet.security.CurrentUserArgumentResolver;
import com.digitalwallet.security.CustomUserDetailsService;
import com.digitalwallet.security.CustomUserPrincipal;
import com.digitalwallet.security.SecurityErrorResponseWriter;
import com.digitalwallet.security.jwt.JwtTokenProvider;
import com.digitalwallet.security.jwt.TokenBlacklistService;
import com.digitalwallet.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * DWS-149: xác nhận @NoHtml chặn HTML/script trong field text tự do của UpdateProfileRequest.
 */
@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, SecurityErrorResponseWriter.class, WebMvcConfig.class,
        CurrentUserArgumentResolver.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @Test
    void updateProfile_fullNameContainsHtml_returns400() throws Exception {
        User user = user("alice@example.com");

        mockMvc.perform(patch("/api/users/me")
                        .with(authentication(userAuthentication(user)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"full_name\":\"<script>alert(1)</script>\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.field").value("full_name"));

        verifyNoInteractions(userService);
    }

    @Test
    void updateProfile_phoneNumberContainsHtml_returns400() throws Exception {
        User user = user("alice@example.com");

        mockMvc.perform(patch("/api/users/me")
                        .with(authentication(userAuthentication(user)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"full_name\":\"Nguyen Van A\",\"phone_number\":\"<script>alert(1)</script>\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.field").value("phone_number"));

        verifyNoInteractions(userService);
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
        user.setRole(UserRole.USER);
        user.setIsActive(true);
        return user;
    }
}
