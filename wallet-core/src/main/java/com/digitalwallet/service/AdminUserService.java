package com.digitalwallet.service;

import com.digitalwallet.api.dto.response.AdminUserResponse;
import com.digitalwallet.api.dto.response.AuditLogResponse;
import com.digitalwallet.domain.entity.User;
import com.digitalwallet.domain.enums.AuditAction;
import com.digitalwallet.domain.enums.AuditActorType;
import com.digitalwallet.domain.enums.UserRole;
import com.digitalwallet.domain.repository.AuditLogRepository;
import com.digitalwallet.domain.repository.UserRepository;
import com.digitalwallet.domain.repository.UserSessionRepository;
import com.digitalwallet.exception.BusinessException;
import com.digitalwallet.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;

    public AdminUserService(UserRepository userRepository, UserSessionRepository userSessionRepository,
            AuditLogRepository auditLogRepository, AuditService auditService) {
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.auditLogRepository = auditLogRepository;
        this.auditService = auditService;
    }

    public Page<AdminUserResponse> listUsers(String email, Pageable pageable) {
        Page<User> users = email == null || email.isBlank()
                ? userRepository.findAll(pageable)
                : userRepository.findByEmailContainingIgnoreCase(email.trim(), pageable);
        return users.map(AdminUserResponse::new);
    }

    public AdminUserResponse getUser(UUID userId) {
        return new AdminUserResponse(findUser(userId));
    }

    @Transactional
    public AdminUserResponse updateRole(User admin, UUID userId, UserRole role,
            HttpServletRequest request, UUID requestId) {
        User target = findUser(userId);
        target.setRole(role);
        target.incrementTokenVersion();
        User saved = userRepository.save(target);
        userSessionRepository.revokeActiveByUserId(saved.getId(), Instant.now());
        audit(admin, AuditAction.USER_ROLE_CHANGED, saved, request, requestId);
        return new AdminUserResponse(saved);
    }

    @Transactional
    public AdminUserResponse updateStatus(User admin, UUID userId, boolean active,
            HttpServletRequest request, UUID requestId) {
        User target = findUser(userId);
        target.setIsActive(active);
        target.incrementTokenVersion();
        User saved = userRepository.save(target);
        if (!active) {
            userSessionRepository.revokeActiveByUserId(saved.getId(), Instant.now());
        }
        audit(admin, AuditAction.USER_STATUS_CHANGED, saved, request, requestId);
        return new AdminUserResponse(saved);
    }

    public Page<AuditLogResponse> listAuditLogs(UUID userId, Pageable pageable) {
        if (userId == null) {
            return auditLogRepository.findAll(pageable).map(AuditLogResponse::new);
        }
        return auditLogRepository.findByResourceTypeAndResourceId("USER", userId, pageable)
                .map(AuditLogResponse::new);
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private void audit(User admin, AuditAction action, User target, HttpServletRequest request, UUID requestId) {
        auditService.log(admin, AuditActorType.ADMIN, action, "USER", target.getId(),
                clientIp(request), request == null ? null : request.getHeader("User-Agent"), requestId);
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