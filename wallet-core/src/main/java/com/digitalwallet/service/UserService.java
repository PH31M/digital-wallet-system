package com.digitalwallet.service;

import com.digitalwallet.api.dto.request.ChangePasswordRequest;
import com.digitalwallet.api.dto.request.UpdateProfileRequest;
import com.digitalwallet.api.dto.response.UserProfileResponse;
import com.digitalwallet.api.dto.response.UserSessionResponse;
import com.digitalwallet.domain.entity.User;
import com.digitalwallet.domain.entity.UserSession;
import com.digitalwallet.domain.enums.AuditAction;
import com.digitalwallet.domain.enums.AuditActorType;
import com.digitalwallet.domain.repository.UserRepository;
import com.digitalwallet.domain.repository.UserSessionRepository;
import com.digitalwallet.exception.BusinessException;
import com.digitalwallet.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this(userRepository, null, passwordEncoder, null);
    }

    @Autowired
    public UserService(UserRepository userRepository, UserSessionRepository userSessionRepository,
            PasswordEncoder passwordEncoder, AuditService auditService) {
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    public UserProfileResponse getProfile(User currentUser) {
        return toProfileResponse(currentUser);
    }

    @Transactional
    public UserProfileResponse updateProfile(User currentUser, UpdateProfileRequest request) {
        currentUser.updateProfile(request.getFullName(), request.getPhoneNumber());
        User savedUser = userRepository.save(currentUser);
        return toProfileResponse(savedUser);
    }

    @Transactional
    public void changePassword(User currentUser, ChangePasswordRequest request) {
        changePassword(currentUser, request, null, null);
    }

    @Transactional
    public void changePassword(User currentUser, ChangePasswordRequest request,
            HttpServletRequest httpRequest, UUID requestId) {
        if (!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CURRENT_PASSWORD);
        }

        currentUser.changePassword(passwordEncoder.encode(request.getNewPassword()));
        User savedUser = userRepository.save(currentUser);
        audit(savedUser, AuditAction.PASSWORD_CHANGED, httpRequest, requestId);
    }

    public List<UserSessionResponse> listSessions(User currentUser) {
        if (userSessionRepository == null) {
            return List.of();
        }
        return userSessionRepository.findActiveByUserId(currentUser.getId(), Instant.now()).stream()
                .map(UserSessionResponse::new)
                .toList();
    }

    @Transactional
    public void revokeSession(User currentUser, UUID sessionId, HttpServletRequest httpRequest, UUID requestId) {
        if (userSessionRepository == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        UserSession session = userSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!session.getUser().getId().equals(currentUser.getId())) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        session.revoke();
        userSessionRepository.save(session);
        audit(currentUser, AuditAction.SESSION_REVOKED, httpRequest, requestId);
    }

    @Transactional
    public void setMfa(User currentUser, boolean enabled, HttpServletRequest httpRequest, UUID requestId) {
        currentUser.setMfaEnabled(enabled);
        User savedUser = userRepository.save(currentUser);
        audit(savedUser, enabled ? AuditAction.MFA_ENABLED : AuditAction.MFA_DISABLED, httpRequest, requestId);
    }

    private void audit(User user, AuditAction action, HttpServletRequest httpRequest, UUID requestId) {
        if (auditService == null) {
            return;
        }
        auditService.log(user, AuditActorType.USER, action, "USER", user.getId(),
                clientIp(httpRequest), httpRequest == null ? null : httpRequest.getHeader("User-Agent"), requestId);
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

    private UserProfileResponse toProfileResponse(User user) {
        return new UserProfileResponse(user.getId(), user.getEmail(), user.getFullName(),
                user.getPhoneNumber(), user.getEmailVerifiedAt() != null);
    }
}
