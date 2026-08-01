package com.digitalwallet.service;

import com.digitalwallet.api.dto.request.RegisterRequest;
import com.digitalwallet.api.dto.request.LoginRequest;
import com.digitalwallet.api.dto.request.RefreshTokenRequest;
import com.digitalwallet.api.dto.request.ResendVerificationRequest;
import com.digitalwallet.api.dto.response.AuthResponse;
import com.digitalwallet.domain.entity.User;
import com.digitalwallet.domain.entity.Wallet;
import com.digitalwallet.domain.enums.AuditAction;
import com.digitalwallet.domain.enums.AuditActorType;
import com.digitalwallet.domain.enums.UserRole;
import com.digitalwallet.domain.enums.WalletStatus;
import com.digitalwallet.domain.repository.UserRepository;
import com.digitalwallet.domain.repository.WalletRepository;
import com.digitalwallet.exception.BusinessException;
import com.digitalwallet.exception.ErrorCode;
import com.digitalwallet.security.jwt.JwtTokenProvider;
import com.digitalwallet.security.jwt.TokenBlacklistService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private AuditService auditService;

    @Mock
    private EmailOtpService emailOtpService;

    @Mock
    private OtpService otpService;

    @Mock
    private HttpServletRequest httpRequest;

    @Test
    void register_happyPath_returnsTokensAndUserInfo() {
        AuthService authService = authService();
        RegisterRequest request = new RegisterRequest("USER@example.com", "Nguyen Van A", "Str0ng@Pass");
        UUID userId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Str0ng@Pass")).thenReturn("bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(userId);
            return user;
        });
        when(jwtTokenProvider.generateAccessToken(any(User.class))).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any(User.class))).thenReturn("refresh-token");
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpRequest.getHeader("User-Agent")).thenReturn("JUnit");

        AuthResponse response = authService.register(request, httpRequest, requestId);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getUser().getId()).isEqualTo(userId);
        assertThat(response.getUser().getEmail()).isEqualTo("user@example.com");
        assertThat(response.getUser().getFullName()).isEqualTo("Nguyen Van A");
        assertThat(response.getUser().isVerified()).isFalse();

        verify(auditService).log(any(User.class), eq(AuditActorType.USER), eq(AuditAction.USER_REGISTERED),
                eq("USER"), eq(userId), eq("127.0.0.1"), eq("JUnit"), eq(requestId));
        verify(emailOtpService).sendRegistrationOtp(any(User.class));
    }

    @Test
    void register_emailAlreadyExists_throwsEmailAlreadyExists() {
        AuthService authService = authService();
        RegisterRequest request = new RegisterRequest("dup@example.com", "Nguyen Van A", "Str0ng@Pass");

        when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request, httpRequest, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);

        verify(userRepository, never()).save(any(User.class));
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void register_autoCreatesDefaultPendingVndWallet() {
        AuthService authService = authService();
        RegisterRequest request = new RegisterRequest("wallet@example.com", "Nguyen Van B", "Str0ng@Pass");
        UUID userId = UUID.randomUUID();

        when(userRepository.existsByEmail("wallet@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Str0ng@Pass")).thenReturn("bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(userId);
            user.setRole(UserRole.USER);
            return user;
        });
        when(jwtTokenProvider.generateAccessToken(any(User.class))).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any(User.class))).thenReturn("refresh-token");

        authService.register(request, httpRequest, UUID.randomUUID());

        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(walletCaptor.capture());
        Wallet wallet = walletCaptor.getValue();

        assertThat(wallet.getUser().getId()).isEqualTo(userId);
        assertThat(wallet.getCurrency()).isEqualTo("VND");
        assertThat(wallet.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(wallet.getStatus()).isEqualTo(WalletStatus.PENDING_VERIFICATION);
    }

    @Test
    void login_happyPath_returnsTokensAndResetsLoginState() {
        AuthService authService = authService();
        LoginRequest request = new LoginRequest("USER@example.com", "Str0ng@Pass");
        User user = user("user@example.com", "Nguyen Van A", "bcrypt-hash");
        user.setFailedLoginAttempts(2);
        UUID requestId = UUID.randomUUID();

        when(userRepository.findByEmail("user@example.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("Str0ng@Pass", "bcrypt-hash")).thenReturn(true);
        when(userRepository.save(user)).thenReturn(user);
        when(jwtTokenProvider.generateAccessToken(user)).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(user)).thenReturn("refresh-token");
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpRequest.getHeader("User-Agent")).thenReturn("JUnit");

        AuthResponse response = authService.login(request, httpRequest, requestId);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getUser().getEmail()).isEqualTo("user@example.com");
        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getLockedUntil()).isNull();
        assertThat(user.getLastLoginAt()).isNotNull();
        verify(auditService).log(user, AuditActorType.USER, AuditAction.USER_LOGIN_SUCCESS,
                "USER", user.getId(), "127.0.0.1", "JUnit", requestId);
    }

    @Test
    void login_invalidPassword_recordsFailedLoginAndThrowsInvalidCredentials() {
        AuthService authService = authService();
        LoginRequest request = new LoginRequest("user@example.com", "wrong-password");
        User user = user("user@example.com", "Nguyen Van A", "bcrypt-hash");
        UUID requestId = UUID.randomUUID();

        when(userRepository.findByEmail("user@example.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "bcrypt-hash")).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpRequest.getHeader("User-Agent")).thenReturn("JUnit");

        assertThatThrownBy(() -> authService.login(request, httpRequest, requestId))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

        assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
        verify(userRepository).save(user);
        verify(auditService).log(user, AuditActorType.USER, AuditAction.USER_LOGIN_FAILED,
                "USER", user.getId(), "127.0.0.1", "JUnit", requestId);
    }

    @Test
    void login_emailNotFound_throwsInvalidCredentials_sameAsWrongPassword() {
        // Must not leak whether the email is registered (email enumeration protection):
        // unknown email and wrong password both surface as INVALID_CREDENTIALS.
        AuthService authService = authService();
        LoginRequest request = new LoginRequest("nobody@example.com", "Str0ng@Pass");

        when(userRepository.findByEmail("nobody@example.com")).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> authService.login(request, httpRequest, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

        verify(passwordEncoder, never()).matches(any(), any());
        verify(userRepository, never()).save(any(User.class));
        verify(auditService, never()).log(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void login_belowLockoutThreshold_incrementsCounterButDoesNotLock() {
        AuthService authService = authService();
        LoginRequest request = new LoginRequest("user@example.com", "wrong-password");
        User user = user("user@example.com", "Nguyen Van A", "bcrypt-hash");
        user.setFailedLoginAttempts(3); // 4th failure should not lock (threshold is 5)

        when(userRepository.findByEmail("user@example.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "bcrypt-hash")).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpRequest.getHeader("User-Agent")).thenReturn("JUnit");

        assertThatThrownBy(() -> authService.login(request, httpRequest, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class);

        assertThat(user.getFailedLoginAttempts()).isEqualTo(4);
        assertThat(user.getLockedUntil()).isNull();
    }

    @Test
    void login_fifthConsecutiveFailure_locksAccountForFifteenMinutes() {
        AuthService authService = authService();
        LoginRequest request = new LoginRequest("user@example.com", "wrong-password");
        User user = user("user@example.com", "Nguyen Van A", "bcrypt-hash");
        user.setFailedLoginAttempts(4); // this failure is the 5th

        when(userRepository.findByEmail("user@example.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "bcrypt-hash")).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpRequest.getHeader("User-Agent")).thenReturn("JUnit");

        Instant beforeCall = Instant.now();
        assertThatThrownBy(() -> authService.login(request, httpRequest, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class);

        assertThat(user.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(user.getLockedUntil()).isNotNull();
        assertThat(user.getLockedUntil()).isAfter(beforeCall.plusSeconds(14 * 60));
        assertThat(user.getLockedUntil()).isBefore(beforeCall.plusSeconds(16 * 60));
    }

    @Test
    void login_subsequentAttemptWhileLocked_neverTouchesPasswordOrIncrementsCounterFurther() {
        AuthService authService = authService();
        LoginRequest request = new LoginRequest("user@example.com", "Str0ng@Pass");
        User user = user("user@example.com", "Nguyen Van A", "bcrypt-hash");
        user.setFailedLoginAttempts(5);
        user.setLockedUntil(Instant.now().plusSeconds(600));

        when(userRepository.findByEmail("user@example.com")).thenReturn(java.util.Optional.of(user));

        assertThatThrownBy(() -> authService.login(request, httpRequest, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_LOCKED);

        assertThat(user.getFailedLoginAttempts()).isEqualTo(5);
        verify(passwordEncoder, never()).matches(any(), any());
        verify(auditService, never()).log(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void register_passwordIsBcryptEncoded_rawPasswordNeverPersisted() {
        AuthService authService = authService();
        RegisterRequest request = new RegisterRequest("hash@example.com", "Nguyen Van A", "Str0ng@Pass");

        when(userRepository.existsByEmail("hash@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Str0ng@Pass")).thenReturn("$2a$12$encodedHashValue");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(jwtTokenProvider.generateAccessToken(any(User.class))).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any(User.class))).thenReturn("refresh-token");
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpRequest.getHeader("User-Agent")).thenReturn("JUnit");

        authService.register(request, httpRequest, UUID.randomUUID());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("$2a$12$encodedHashValue");
        assertThat(userCaptor.getValue().getPasswordHash()).isNotEqualTo("Str0ng@Pass");
        verify(passwordEncoder).encode("Str0ng@Pass");
    }

    @Test
    void register_forwardedForHeaderPresent_usesFirstIpForAuditLog() {
        AuthService authService = authService();
        RegisterRequest request = new RegisterRequest("proxy@example.com", "Nguyen Van A", "Str0ng@Pass");
        UUID userId = UUID.randomUUID();

        when(userRepository.existsByEmail("proxy@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Str0ng@Pass")).thenReturn("bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(userId);
            return user;
        });
        when(jwtTokenProvider.generateAccessToken(any(User.class))).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any(User.class))).thenReturn("refresh-token");
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn("198.51.100.9, 10.0.0.1");
        when(httpRequest.getHeader("User-Agent")).thenReturn("JUnit");

        authService.register(request, httpRequest, UUID.randomUUID());

        verify(auditService).log(any(User.class), eq(AuditActorType.USER), eq(AuditAction.USER_REGISTERED),
                eq("USER"), eq(userId), eq("198.51.100.9"), eq("JUnit"), any());
        verify(httpRequest, never()).getRemoteAddr();
    }

    @Test
    void login_lockedAccount_throwsAccountLocked() {
        AuthService authService = authService();
        LoginRequest request = new LoginRequest("user@example.com", "Str0ng@Pass");
        User user = user("user@example.com", "Nguyen Van A", "bcrypt-hash");
        user.setLockedUntil(Instant.now().plusSeconds(60));

        when(userRepository.findByEmail("user@example.com")).thenReturn(java.util.Optional.of(user));

        assertThatThrownBy(() -> authService.login(request, httpRequest, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACCOUNT_LOCKED);

        verify(passwordEncoder, never()).matches(any(), any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void refreshToken_validToken_blacklistsOldRefreshAndReturnsNewTokenPair() {
        AuthService authService = authService();
        RefreshTokenRequest request = new RefreshTokenRequest("old-refresh-token");
        User user = user("user@example.com", "Nguyen Van A", "bcrypt-hash");
        user.setTokenVersion(0);

        when(jwtTokenProvider.isValid("old-refresh-token")).thenReturn(true);
        when(jwtTokenProvider.getTokenType("old-refresh-token")).thenReturn("refresh");
        when(jwtTokenProvider.getEmail("old-refresh-token")).thenReturn("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(tokenBlacklistService.isBlacklisted("old-refresh-token")).thenReturn(false);
        when(jwtTokenProvider.getTokenVersion("old-refresh-token")).thenReturn(0);
        when(jwtTokenProvider.getExpiration("old-refresh-token")).thenReturn(Instant.now().plusSeconds(3600));
        when(jwtTokenProvider.generateAccessToken(user)).thenReturn("new-access-token");
        when(jwtTokenProvider.generateRefreshToken(user)).thenReturn("new-refresh-token");

        AuthResponse response = authService.refreshToken(request, httpRequest, UUID.randomUUID());

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
        assertThat(response.getUser().getEmail()).isEqualTo("user@example.com");

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(tokenBlacklistService).blacklist(eq("old-refresh-token"), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isPositive();
        verify(userRepository, never()).save(user);
    }

    @Test
    void refreshToken_blacklistedToken_detectsReuseIncrementsVersionAuditsAndThrows401() {
        AuthService authService = authService();
        RefreshTokenRequest request = new RefreshTokenRequest("reused-refresh-token");
        User user = user("user@example.com", "Nguyen Van A", "bcrypt-hash");
        user.setTokenVersion(0);
        UUID requestId = UUID.randomUUID();

        when(jwtTokenProvider.isValid("reused-refresh-token")).thenReturn(true);
        when(jwtTokenProvider.getTokenType("reused-refresh-token")).thenReturn("refresh");
        when(jwtTokenProvider.getEmail("reused-refresh-token")).thenReturn("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(tokenBlacklistService.isBlacklisted("reused-refresh-token")).thenReturn(true);
        when(userRepository.save(user)).thenReturn(user);
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpRequest.getHeader("User-Agent")).thenReturn("JUnit");

        assertThatThrownBy(() -> authService.refreshToken(request, httpRequest, requestId))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.TOKEN_REUSE_DETECTED);

        assertThat(user.getTokenVersion()).isEqualTo(1);
        verify(userRepository).save(user);
        verify(auditService).log(user, AuditActorType.USER, AuditAction.SECURITY_ALERT,
                "USER", user.getId(), "127.0.0.1", "JUnit", requestId);
        verify(tokenBlacklistService, never()).blacklist(any(), any());
        verify(jwtTokenProvider, never()).generateAccessToken(any(User.class));
        verify(jwtTokenProvider, never()).generateRefreshToken(any(User.class));
    }

    @Test
    void refreshToken_tokenVersionMismatch_rejectsRevokedGeneration() {
        AuthService authService = authService();
        RefreshTokenRequest request = new RefreshTokenRequest("old-generation-refresh-token");
        User user = user("user@example.com", "Nguyen Van A", "bcrypt-hash");
        user.setTokenVersion(2);

        when(jwtTokenProvider.isValid("old-generation-refresh-token")).thenReturn(true);
        when(jwtTokenProvider.getTokenType("old-generation-refresh-token")).thenReturn("refresh");
        when(jwtTokenProvider.getEmail("old-generation-refresh-token")).thenReturn("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(tokenBlacklistService.isBlacklisted("old-generation-refresh-token")).thenReturn(false);
        when(jwtTokenProvider.getTokenVersion("old-generation-refresh-token")).thenReturn(1);

        assertThatThrownBy(() -> authService.refreshToken(request, httpRequest, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.TOKEN_EXPIRED);

        verify(tokenBlacklistService, never()).blacklist(any(), any());
        verify(userRepository, never()).save(any(User.class));
        verify(auditService, never()).log(any(), any(), any(), any(), any(), any(), any(), any());
        verify(jwtTokenProvider, never()).generateAccessToken(any(User.class));
        verify(jwtTokenProvider, never()).generateRefreshToken(any(User.class));
    }

    @Test
    void logout_validTokenWithCurrentAccessToken_blacklistsBothTokensAndLogsAudit() {
        AuthService authService = authService();
        User user = user("user@example.com", "Nguyen Van A", "bcrypt-hash");

        when(jwtTokenProvider.getExpiration("valid-refresh-token")).thenReturn(Instant.now().plusSeconds(500000));
        when(jwtTokenProvider.getEmail("valid-refresh-token")).thenReturn("user@example.com");
        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer valid-access-token");
        when(jwtTokenProvider.isValid("valid-access-token")).thenReturn(true);
        when(jwtTokenProvider.isAccessToken("valid-access-token")).thenReturn(true);
        when(jwtTokenProvider.getExpiration("valid-access-token")).thenReturn(Instant.now().plusSeconds(900));
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpRequest.getHeader("User-Agent")).thenReturn("JUnit");

        authService.logout("valid-refresh-token", httpRequest);

        verify(tokenBlacklistService).blacklist(eq("valid-refresh-token"), any(Duration.class));
        verify(tokenBlacklistService).blacklist(eq("valid-access-token"), any(Duration.class));
        verify(auditService).log(user, AuditActorType.USER, AuditAction.USER_LOGOUT,
                "USER", user.getId(), "127.0.0.1", "JUnit", null);
    }

    @Test
    void logout_validToken_blacklistsTokenAndLogsAudit() {
        AuthService authService = authService();
        User user = user("user@example.com", "Nguyen Van A", "bcrypt-hash");

        when(jwtTokenProvider.getExpiration("valid-refresh-token")).thenReturn(Instant.now().plusSeconds(500000));
        when(jwtTokenProvider.getEmail("valid-refresh-token")).thenReturn("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpRequest.getHeader("User-Agent")).thenReturn("JUnit");

        authService.logout("valid-refresh-token", httpRequest);

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(tokenBlacklistService).blacklist(eq("valid-refresh-token"), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isPositive();
        verify(auditService).log(user, AuditActorType.USER, AuditAction.USER_LOGOUT,
                "USER", user.getId(), "127.0.0.1", "JUnit", null);
    }

    @Test
    void logout_whenCurrentAccessTokenBlacklistFails_propagatesFailureAndDoesNotAuditSuccess() {
        AuthService authService = authService();

        when(jwtTokenProvider.getExpiration("valid-refresh-token")).thenReturn(Instant.now().plusSeconds(500000));
        when(jwtTokenProvider.getEmail("valid-refresh-token")).thenReturn("user@example.com");
        when(httpRequest.getHeader("Authorization")).thenReturn("Bearer valid-access-token");
        when(jwtTokenProvider.isValid("valid-access-token")).thenReturn(true);
        when(jwtTokenProvider.isAccessToken("valid-access-token")).thenReturn(true);
        when(jwtTokenProvider.getExpiration("valid-access-token")).thenReturn(Instant.now().plusSeconds(900));
        org.mockito.Mockito.doThrow(new BusinessException(ErrorCode.TOKEN_BLACKLIST_UNAVAILABLE))
                .when(tokenBlacklistService).blacklist(eq("valid-access-token"), any(Duration.class));

        assertThatThrownBy(() -> authService.logout("valid-refresh-token", httpRequest))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.TOKEN_BLACKLIST_UNAVAILABLE);

        verify(tokenBlacklistService).blacklist(eq("valid-refresh-token"), any(Duration.class));
        verify(userRepository, never()).findByEmail(any());
        verify(auditService, never()).log(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void logout_whenBlacklistFails_propagatesFailureAndDoesNotAuditSuccess() {
        AuthService authService = authService();

        when(jwtTokenProvider.getExpiration("valid-refresh-token")).thenReturn(Instant.now().plusSeconds(500000));
        when(jwtTokenProvider.getEmail("valid-refresh-token")).thenReturn("user@example.com");
        org.mockito.Mockito.doThrow(new BusinessException(ErrorCode.TOKEN_BLACKLIST_UNAVAILABLE))
                .when(tokenBlacklistService).blacklist(eq("valid-refresh-token"), any(Duration.class));

        assertThatThrownBy(() -> authService.logout("valid-refresh-token", httpRequest))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.TOKEN_BLACKLIST_UNAVAILABLE);

        verify(userRepository, never()).findByEmail(any());
        verify(auditService, never()).log(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void logout_malformedToken_stillSucceedsWithoutThrowing() {
        AuthService authService = authService();

        when(jwtTokenProvider.getExpiration("garbage-token")).thenThrow(new IllegalArgumentException("bad token"));

        assertThatCode(() -> authService.logout("garbage-token", httpRequest))
                .doesNotThrowAnyException();

        verify(tokenBlacklistService, never()).blacklist(any(), any());
        verify(auditService, never()).log(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void resendVerification_emailMismatch_rejectsRequest() {
        AuthService authService = authService();
        User user = user("user@example.com", "Nguyen Van A", "bcrypt-hash");
        ResendVerificationRequest request = new ResendVerificationRequest(user.getId(), "other@example.com");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.resendVerification(request, httpRequest))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_VERIFICATION_REQUEST);

        verify(otpService, never()).tryConsumeResendQuota(any());
        verify(emailOtpService, never()).sendRegistrationOtp(any());
    }

    @Test
    void resendVerification_alreadyVerified_rejectsRequest() {
        AuthService authService = authService();
        User user = user("user@example.com", "Nguyen Van A", "bcrypt-hash");
        user.setEmailVerifiedAt(Instant.now());
        ResendVerificationRequest request = new ResendVerificationRequest(user.getId(), "user@example.com");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.resendVerification(request, httpRequest))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.EMAIL_ALREADY_VERIFIED);

        verify(otpService, never()).tryConsumeResendQuota(any());
        verify(emailOtpService, never()).sendRegistrationOtp(any());
    }

    @Test
    void resendVerification_ipQuotaExceeded_rejectsBeforeUserQuota() {
        AuthService authService = authService();
        User user = user("user@example.com", "Nguyen Van A", "bcrypt-hash");
        ResendVerificationRequest request = new ResendVerificationRequest(user.getId(), "USER@example.com");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpRequest.getRemoteAddr()).thenReturn("203.0.113.10");
        when(otpService.tryConsumeResendIpQuota("203.0.113.10")).thenReturn(false);

        assertThatThrownBy(() -> authService.resendVerification(request, httpRequest))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.RESEND_RATE_LIMITED);

        verify(otpService, never()).tryConsumeResendQuota(any());
        verify(emailOtpService, never()).sendRegistrationOtp(any());
    }

    @Test
    void resendVerification_validRequest_consumesIpAndUserQuotaThenSendsOtp() {
        AuthService authService = authService();
        User user = user("user@example.com", "Nguyen Van A", "bcrypt-hash");
        ResendVerificationRequest request = new ResendVerificationRequest(user.getId(), "USER@example.com");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn("198.51.100.9, 10.0.0.1");
        when(otpService.tryConsumeResendIpQuota("198.51.100.9")).thenReturn(true);
        when(otpService.tryConsumeResendQuota(user.getId())).thenReturn(true);

        authService.resendVerification(request, httpRequest);

        verify(emailOtpService).sendRegistrationOtp(user);
    }

    private AuthService authService() {
        return new AuthService(userRepository, walletRepository, passwordEncoder, jwtTokenProvider,
                tokenBlacklistService, auditService, emailOtpService, otpService);
    }

    private User user(String email, String fullName, String passwordHash) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPasswordHash(passwordHash);
        user.setRole(UserRole.USER);
        user.setIsActive(true);
        user.setFailedLoginAttempts(0);
        return user;
    }
}
