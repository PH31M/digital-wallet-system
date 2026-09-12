package com.digitalwallet.service;

import com.digitalwallet.api.dto.response.AdminUserResponse;
import com.digitalwallet.api.dto.response.AuditLogResponse;
import com.digitalwallet.api.dto.response.UserSessionResponse;
import com.digitalwallet.domain.entity.AuditLog;
import com.digitalwallet.domain.entity.User;
import com.digitalwallet.domain.entity.UserSession;
import com.digitalwallet.domain.enums.AuditAction;
import com.digitalwallet.domain.enums.AuditActorType;
import com.digitalwallet.domain.enums.UserRole;
import com.digitalwallet.domain.repository.AuditLogRepository;
import com.digitalwallet.domain.repository.AuditLogSpecifications;
import com.digitalwallet.domain.repository.UserRepository;
import com.digitalwallet.domain.repository.UserSessionRepository;
import com.digitalwallet.exception.BusinessException;
import com.digitalwallet.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AdminUserService {

    private static final Set<String> ALLOWED_USER_SORT_FIELDS = Set.of("createdAt", "email", "fullName", "role");
    private static final Set<String> ALLOWED_AUDIT_SORT_FIELDS = Set.of("createdAt", "actionType", "actorType");

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
        Pageable sanitized = sanitizeSort(pageable, ALLOWED_USER_SORT_FIELDS);
        Page<User> users = email == null || email.isBlank()
                ? userRepository.findAll(sanitized)
                : userRepository.findByEmailContainingIgnoreCase(email.trim(), sanitized);
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

    public Page<AuditLogResponse> listAuditLogs(UUID userId, AuditAction action, AuditActorType actorType,
            Instant from, Instant to, Pageable pageable) {
        Pageable sanitized = sanitizeSort(pageable, ALLOWED_AUDIT_SORT_FIELDS);
        if (userId != null) {
            return auditLogRepository.findByResourceTypeAndResourceId("USER", userId, sanitized)
                    .map(AuditLogResponse::new);
        }
        Specification<AuditLog> spec = AuditLogSpecifications.filter(action, actorType, null, from, to);
        return auditLogRepository.findAll(spec, sanitized).map(AuditLogResponse::new);
    }

    public void exportAuditLogsCsv(Instant from, Instant to, PrintWriter writer) {
        Specification<AuditLog> spec = AuditLogSpecifications.filter(null, null, null, from, to);
        int pageSize = 500;
        int pageNumber = 0;
        writer.println("id,created_at,actor_id,actor_type,action,resource_type,resource_id,ip_address,user_agent,request_id");
        Page<AuditLog> page;
        do {
            Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.ASC, "createdAt"));
            page = auditLogRepository.findAll(spec, pageable);
            for (AuditLog log : page.getContent()) {
                writer.println(toCsvRow(log));
            }
            pageNumber++;
        } while (page.hasNext());
    }

    private String toCsvRow(AuditLog log) {
        return String.join(",",
                String.valueOf(log.getId()),
                String.valueOf(log.getCreatedAt()),
                log.getActor() == null ? "" : String.valueOf(log.getActor().getId()),
                String.valueOf(log.getActorType()),
                String.valueOf(log.getAction()),
                escapeCsv(log.getResourceType()),
                String.valueOf(log.getResourceId()),
                escapeCsv(log.getIpAddress()),
                escapeCsv(log.getUserAgent()),
                String.valueOf(log.getRequestId()));
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    public List<UserSessionResponse> listUserSessions(UUID userId) {
        findUser(userId);
        return userSessionRepository.findActiveByUserId(userId, Instant.now())
                .stream().map(UserSessionResponse::new).toList();
    }

    @Transactional
    public void revokeUserSession(User admin, UUID userId, UUID sessionId,
            HttpServletRequest request, UUID requestId) {
        User target = findUser(userId);
        UserSession session = userSessionRepository.findById(sessionId)
                .filter(s -> s.getUser().getId().equals(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        session.revoke();
        userSessionRepository.save(session);
        audit(admin, AuditAction.SESSION_REVOKED, target, request, requestId);
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private Pageable sanitizeSort(Pageable pageable, Set<String> allowedFields) {
        List<Sort.Order> validOrders = pageable.getSort().stream()
                .filter(order -> allowedFields.contains(order.getProperty()))
                .toList();
        Sort sort = validOrders.isEmpty() ? Sort.by(Sort.Direction.DESC, "createdAt") : Sort.by(validOrders);
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
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