package com.digitalwallet.api.controller;

import com.digitalwallet.api.dto.response.AuthResponse;
import com.digitalwallet.api.dto.response.UserProfileResponse;
import com.digitalwallet.exception.BusinessException;
import com.digitalwallet.exception.EmailAlreadyExistsException;
import com.digitalwallet.exception.ErrorCode;
import com.digitalwallet.common.request.RequestIds;
import com.digitalwallet.security.CustomUserDetailsService;
import com.digitalwallet.security.jwt.JwtTokenProvider;
import com.digitalwallet.security.jwt.TokenBlacklistService;
import com.digitalwallet.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @Test
    void register_weakPassword_returnsWeakPasswordError() throws Exception {
        String body = """
                {
                  "email": "test@example.com",
                  "full_name": "Nguyen Van A",
                  "password": "weak"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("WEAK_PASSWORD"))
                .andExpect(jsonPath("$.error.field").value("password"));

        verifyNoInteractions(authService);
    }

    @Test
    void register_emailAlreadyExists_returnsEmailAlreadyExistsError() throws Exception {
        String body = """
                {
                  "email": "dup@example.com",
                  "full_name": "Nguyen Van A",
                  "password": "Str0ng@Pass"
                }
                """;

        when(authService.register(any(), any(), any()))
                .thenThrow(new EmailAlreadyExistsException("Email already exists"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void login_happyPath_returnsAuthResponse() throws Exception {
        String body = """
                {
                  "email": "test@example.com",
                  "password": "Str0ng@Pass"
                }
                """;
        AuthResponse response = new AuthResponse("access-token", "refresh-token",
                new UserProfileResponse(UUID.randomUUID(), "test@example.com", "Nguyen Van A", false));

        when(authService.login(any(), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.access_token").value("access-token"))
                .andExpect(jsonPath("$.data.refresh_token").value("refresh-token"))
                .andExpect(jsonPath("$.data.user.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.user.password").doesNotExist())
                .andExpect(jsonPath("$.data.user.password_hash").doesNotExist())
                .andExpect(jsonPath("$.requestId").exists())
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void login_accountLocked_returns403WithAccountLockedCode() throws Exception {
        String body = """
                {
                  "email": "test@example.com",
                  "password": "Str0ng@Pass"
                }
                """;

        when(authService.login(any(), any(), any()))
                .thenThrow(new BusinessException(ErrorCode.ACCOUNT_LOCKED));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_LOCKED"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void login_invalidCredentials_returns401WithInvalidCredentialsCode() throws Exception {
        String body = """
                {
                  "email": "test@example.com",
                  "password": "wrong-password"
                }
                """;

        when(authService.login(any(), any(), any()))
                .thenThrow(new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void login_missingPassword_returnsValidationFailedWithFieldPassword() throws Exception {
        String body = """
                {
                  "email": "test@example.com"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.field").value("password"));

        verifyNoInteractions(authService);
    }

    @Test
    void register_malformedEmail_returnsValidationFailedWithFieldEmail() throws Exception {
        String body = """
                {
                  "email": "not-an-email",
                  "full_name": "Nguyen Van A",
                  "password": "Str0ng@Pass"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.field").value("email"));

        verifyNoInteractions(authService);
    }

    @Test
    void register_fullNameTooShort_returnsValidationFailedWithFieldFullName() throws Exception {
        String body = """
                {
                  "email": "test@example.com",
                  "full_name": "A",
                  "password": "Str0ng@Pass"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.field").value("full_name"));
    }

    @Test
    void resendVerification_missingEmail_returnsValidationFailed() throws Exception {
        String body = """
                {
                  "user_id": "11111111-1111-1111-1111-111111111111"
                }
                """;

        mockMvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.field").value("email"));

        verifyNoInteractions(authService);
    }

    @Test
    void resendVerification_validBody_returnsSameRequestIdFromHeader() throws Exception {
        String requestId = UUID.randomUUID().toString();
        String body = """
                {
                  "user_id": "11111111-1111-1111-1111-111111111111",
                  "email": "test@example.com"
                }
                """;

        mockMvc.perform(post("/api/auth/resend-verification")
                        .header(RequestIds.HEADER, requestId)
                        .requestAttr(RequestIds.ATTRIBUTE, requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.requestId").value(requestId));
    }

    @Test
    void register_unexpectedRuntimeException_returns500WithGenericErrorAndNoLeakedDetail() throws Exception {
        String body = """
                {
                  "email": "test@example.com",
                  "full_name": "Nguyen Van A",
                  "password": "Str0ng@Pass"
                }
                """;

        when(authService.register(any(), any(), any()))
                .thenThrow(new RuntimeException("duplicate key value violates unique constraint \"uq_users_email\""));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("uq_users_email"))));
    }
}
