package com.digitalwallet.service;

import com.digitalwallet.api.dto.request.ForgotPasswordRequest;
import com.digitalwallet.api.dto.request.LoginRequest;
import com.digitalwallet.api.dto.request.RefreshTokenRequest;
import com.digitalwallet.api.dto.request.RegisterRequest;
import com.digitalwallet.api.dto.request.ResendVerificationRequest;
import com.digitalwallet.api.dto.request.ResetPasswordRequest;
import com.digitalwallet.api.dto.request.VerifyMfaRequest;
import com.digitalwallet.api.dto.response.AuthResponse;
import com.digitalwallet.api.dto.response.UserProfileResponse;
import com.digitalwallet.domain.entity.User;
import com.digitalwallet.domain.entity.UserSession;
import com.digitalwallet.domain.entity.Wallet;
import com.digitalwallet.domain.enums.AuditAction;
import com.digitalwallet.domain.enums.AuditActorType;
import com.digitalwallet.domain.enums.UserRole;
import com.digitalwallet.domain.enums.WalletStatus;
import com.digitalwallet.domain.repository.UserRepository;
import com.digitalwallet.domain.repository.UserSessionRepository;
import com.digitalwallet.domain.repository.WalletRepository;
import com.digitalwallet.exception.BusinessException;
import com.digitalwallet.exception.EmailAlreadyExistsException;
import com.digitalwallet.exception.ErrorCode;
import com.digitalwallet.security.jwt.JwtTokenProvider;
import com.digitalwallet.security.jwt.TokenBlacklistService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final AuditService auditService;
    private final EmailOtpService emailOtpService;
    private final OtpService otpService;

    public AuthService(UserRepository userRepository, WalletRepository walletRepository,
            PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider,
            TokenBlacklistService tokenBlacklistService, AuditService auditService,
            EmailOtpService emailOtpService, OtpService otpService) {
        this(userRepository, null, walletRepository, passwordEncoder, jwtTokenProvider,
                tokenBlacklistService, auditService, emailOtpService, otpService);
    }

    @Autowired
    public AuthService(UserRepository userRepository, UserSessionRepository userSessionRepository,
            WalletRepository walletRepository, PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider, TokenBlacklistService tokenBlacklistService,
            AuditService auditService, EmailOtpService emailOtpService, OtpService otpService) {
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.walletRepository = walletRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenBlacklistService = tokenBlacklistService;
        this.auditService = auditService;
        this.emailOtpService = emailOtpService;
        this.otpService = otpService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest, UUID requestId) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setFullName(request.getFullName().trim());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.USER);
        user.setIsActive(true);
        user.setFailedLoginAttempts(0);

        User savedUser = userRepository.save(user);

        Wallet wallet = new Wallet();
        wallet.setUser(savedUser);
        wallet.setCurrency("VND");
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setStatus(WalletStatus.PENDING_VERIFICATION);
        walletRepository.save(wallet);

        String accessToken = jwtTokenProvider.generateAccessToken(savedUser);
        String refreshToken = jwtTokenProvider.generateRefreshToken(savedUser);
        createSession(savedUser, refreshToken, httpRequest);

        auditService.log(savedUser, AuditActorType.USER, AuditAction.USER_REGISTERED,
                "USER", savedUser.getId(), clientIp(httpRequest), httpRequest.getHeader("User-Agent"), requestId);
        sendRegistrationOtpAfterCommit(savedUser);

        return new AuthResponse(accessToken, refreshToken,
                new UserProfileResponse(savedUser.getId(), savedUser.getEmail(), savedUser.getFullName(),
                        savedUser.getEmailVerifiedAt() != null));
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request, HttpServletRequest httpRequest, UUID requestId) {
        String refreshToken = request.getRefreshToken();
        validateRefreshToken(refreshToken);

        String email = jwtTokenProvider.getEmail(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_EXPIRED));

        if (tokenBlacklistService.isBlacklisted(refreshToken)) {
            user.incrementTokenVersion();
            User savedUser = userRepository.save(user);
            auditService.log(savedUser, AuditActorType.USER, AuditAction.SECURITY_ALERT,
                    "USER", savedUser.getId(), clientIp(httpRequest), httpRequest.getHeader("User-Agent"), requestId);
            throw new BusinessException(ErrorCode.TOKEN_REUSE_DETECTED);
        }

        Integer tokenVersion = jwtTokenProvider.getTokenVersion(refreshToken);
        if (!Objects.equals(user.getTokenVersion(), tokenVersion)) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        }

        Duration remainingTtl = Duration.between(Instant.now(), jwtTokenProvider.getExpiration(refreshToken));
        tokenBlacklistService.blacklist(refreshToken, remainingTtl);

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user);
        rotateSession(refreshToken, newRefreshToken, user, httpRequest);

        return new AuthResponse(accessToken, newRefreshToken,
                new UserProfileResponse(user.getId(), user.getEmail(), user.getFullName(),
                        user.getEmailVerifiedAt() != null));
    }

    public void logout(String refreshToken, HttpServletRequest httpRequest) {
        Duration remainingTtl;
        String email;

        try {
            remainingTtl = Duration.between(Instant.now(), jwtTokenProvider.getExpiration(refreshToken));
            email = jwtTokenProvider.getEmail(refreshToken);
        } catch (Exception ex) {
            // Logout remains idempotent for tokens that are already malformed or expired.
            return;
        }

        tokenBlacklistService.blacklist(refreshToken, remainingTtl);
        revokeRefreshSession(refreshToken);
        blacklistCurrentAccessToken(httpRequest);

        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            auditService.log(user, AuditActorType.USER, AuditAction.USER_LOGOUT,
                    "USER", user.getId(), clientIp(httpRequest), httpRequest.getHeader("User-Agent"), null);
        }
    }

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest, UUID requestId) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (user.isCurrentlyLocked()) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            user.recordFailedLogin();
            userRepository.save(user);
            auditService.log(user, AuditActorType.USER, AuditAction.USER_LOGIN_FAILED,
                    "USER", user.getId(), clientIp(httpRequest), httpRequest.getHeader("User-Agent"), requestId);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        user.recordSuccessfulLogin();
        User savedUser = userRepository.save(user);

        if (Boolean.TRUE.equals(savedUser.getMfaEnabled())) {
            emailOtpService.sendMfaOtp(savedUser);
            auditService.log(savedUser, AuditActorType.USER, AuditAction.MFA_CHALLENGE_SENT,
                    "USER", savedUser.getId(), clientIp(httpRequest), httpRequest.getHeader("User-Agent"), requestId);
            return new AuthResponse(null, null,
                    new UserProfileResponse(savedUser.getId(), savedUser.getEmail(), savedUser.getFullName(),
                            savedUser.getEmailVerifiedAt() != null),
                    true);
        }

        String accessToken = jwtTokenProvider.generateAccessToken(savedUser);
        String refreshToken = jwtTokenProvider.generateRefreshToken(savedUser);
        createSession(savedUser, refreshToken, httpRequest);

        auditService.log(savedUser, AuditActorType.USER, AuditAction.USER_LOGIN_SUCCESS,
                "USER", savedUser.getId(), clientIp(httpRequest), httpRequest.getHeader("User-Agent"), requestId);

        return new AuthResponse(accessToken, refreshToken,
                new UserProfileResponse(savedUser.getId(), savedUser.getEmail(), savedUser.getFullName(),
                        savedUser.getEmailVerifiedAt() != null));
    }

    @Transactional
    public AuthResponse verifyMfa(VerifyMfaRequest request, HttpServletRequest httpRequest, UUID requestId) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!Boolean.TRUE.equals(user.getMfaEnabled())) {
            throw new BusinessException(ErrorCode.INVALID_VERIFICATION_REQUEST);
        }
        if (otpService.hasExceededMfaAttempts(user.getId())) {
            throw new BusinessException(ErrorCode.OTP_MAX_ATTEMPTS);
        }
        if (!otpService.verifyMfaOtp(user.getId(), request.getOtp())) {
            otpService.incrementMfaAttempts(user.getId());
            throw new BusinessException(ErrorCode.OTP_EXPIRED);
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);
        createSession(user, refreshToken, httpRequest);

        auditService.log(user, AuditActorType.USER, AuditAction.USER_LOGIN_SUCCESS,
                "USER", user.getId(), clientIp(httpRequest), httpRequest.getHeader("User-Agent"), requestId);

        return new AuthResponse(accessToken, refreshToken,
                new UserProfileResponse(user.getId(), user.getEmail(), user.getFullName(),
                        user.getEmailVerifiedAt() != null));
    }

    public void forgotPassword(ForgotPasswordRequest request, HttpServletRequest httpRequest, UUID requestId) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (user == null || !Boolean.TRUE.equals(user.getIsActive())) {
            return;
        }

        if (!otpService.tryConsumeResendIpQuota(clientIp(httpRequest)) || !otpService.tryConsumeResendQuota(user.getId())) {
            throw new BusinessException(ErrorCode.RESEND_RATE_LIMITED);
        }

        emailOtpService.sendPasswordResetOtp(user);
        auditService.log(user, AuditActorType.USER, AuditAction.PASSWORD_RESET_REQUESTED,
                "USER", user.getId(), clientIp(httpRequest), httpRequest.getHeader("User-Agent"), requestId);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request, HttpServletRequest httpRequest, UUID requestId) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.OTP_EXPIRED));

        if (otpService.hasExceededPasswordResetAttempts(user.getId())) {
            throw new BusinessException(ErrorCode.OTP_MAX_ATTEMPTS);
        }
        if (!otpService.verifyPasswordResetOtp(user.getId(), request.getOtp())) {
            otpService.incrementPasswordResetAttempts(user.getId());
            throw new BusinessException(ErrorCode.OTP_EXPIRED);
        }

        user.changePassword(passwordEncoder.encode(request.getNewPassword()));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        User savedUser = userRepository.save(user);
        revokeAllSessions(savedUser);

        auditService.log(savedUser, AuditActorType.USER, AuditAction.PASSWORD_RESET_COMPLETED,
                "USER", savedUser.getId(), clientIp(httpRequest), httpRequest.getHeader("User-Agent"), requestId);
    }

    @Transactional
    public void verifyEmail(UUID userId, String otp, UUID requestId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        if (otpService.hasExceededRegistrationAttempts(userId)) {
            throw new BusinessException(ErrorCode.OTP_MAX_ATTEMPTS);
        }

        if (!otpService.verifyRegistrationOtp(userId, otp)) {
            otpService.incrementRegistrationAttempts(userId);
            throw new BusinessException(ErrorCode.OTP_EXPIRED);
        }

        user.markAsVerified();
        userRepository.save(user);

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        wallet.activate();
        walletRepository.save(wallet);

        auditService.log(user, AuditActorType.USER, AuditAction.EMAIL_VERIFIED,
                "USER", user.getId(), null, null, requestId);
    }

    public void resendVerification(ResendVerificationRequest request, HttpServletRequest httpRequest) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (!user.getEmail().equalsIgnoreCase(normalizedEmail)) {
            throw new BusinessException(ErrorCode.INVALID_VERIFICATION_REQUEST);
        }

        if (user.getEmailVerifiedAt() != null) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_VERIFIED);
        }

        if (!otpService.tryConsumeResendIpQuota(clientIp(httpRequest)) || !otpService.tryConsumeResendQuota(user.getId())) {
            throw new BusinessException(ErrorCode.RESEND_RATE_LIMITED);
        }

        emailOtpService.sendRegistrationOtp(user);
    }

    private void sendRegistrationOtpAfterCommit(User user) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            emailOtpService.sendRegistrationOtp(user);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                emailOtpService.sendRegistrationOtp(user);
            }
        });
    }

    private void validateRefreshToken(String token) {
        if (!jwtTokenProvider.isValid(token) || !"refresh".equals(jwtTokenProvider.getTokenType(token))) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        }
    }

    private void blacklistCurrentAccessToken(HttpServletRequest request) {
        String accessToken = extractBearerToken(request);
        if (accessToken == null || !jwtTokenProvider.isValid(accessToken) || !jwtTokenProvider.isAccessToken(accessToken)) {
            return;
        }

        Duration remainingTtl = Duration.between(Instant.now(), jwtTokenProvider.getExpiration(accessToken));
        tokenBlacklistService.blacklist(accessToken, remainingTtl);
    }

    private void createSession(User user, String refreshToken, HttpServletRequest request) {
        if (userSessionRepository == null || refreshToken == null || !jwtTokenProvider.isValid(refreshToken)) {
            return;
        }

        userSessionRepository.save(UserSession.create(
                user,
                jwtTokenProvider.getJti(refreshToken),
                deviceName(request),
                clientIp(request),
                request == null ? null : request.getHeader("User-Agent"),
                jwtTokenProvider.getExpiration(refreshToken)));
    }

    private void rotateSession(String oldRefreshToken, String newRefreshToken, User user, HttpServletRequest request) {
        if (userSessionRepository == null) {
            return;
        }

        String oldJti = jwtTokenProvider.getJti(oldRefreshToken);
        UserSession oldSession = userSessionRepository.findByRefreshTokenId(oldJti)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_EXPIRED));
        if (!oldSession.isActive() || !oldSession.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        }

        oldSession.markUsed();
        oldSession.revoke();
        userSessionRepository.save(oldSession);
        createSession(user, newRefreshToken, request);
    }

    private void revokeRefreshSession(String refreshToken) {
        if (userSessionRepository == null || refreshToken == null || !jwtTokenProvider.isValid(refreshToken)) {
            return;
        }

        userSessionRepository.findByRefreshTokenId(jwtTokenProvider.getJti(refreshToken)).ifPresent(session -> {
            session.revoke();
            userSessionRepository.save(session);
        });
    }

    private void revokeAllSessions(User user) {
        if (userSessionRepository != null && user != null) {
            userSessionRepository.revokeActiveByUserId(user.getId(), Instant.now());
        }
    }

    private String deviceName(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String device = request.getHeader("X-Device-Name");
        if (device != null && !device.isBlank()) {
            return device.trim();
        }
        String userAgent = request.getHeader("User-Agent");
        return userAgent == null || userAgent.isBlank() ? "unknown" : userAgent;
    }

    private String extractBearerToken(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }

        String token = authorization.substring(7).trim();
        return token.isBlank() ? null : token;
    }

    private String clientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr == null || remoteAddr.isBlank() ? "unknown" : remoteAddr;
    }
}
