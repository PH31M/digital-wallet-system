package com.digitalwallet.api.controller;

import com.digitalwallet.api.dto.request.UpdateUserRoleRequest;
import com.digitalwallet.api.dto.request.UpdateUserStatusRequest;
import com.digitalwallet.api.dto.response.AdminUserResponse;
import com.digitalwallet.api.dto.response.AuditLogResponse;
import com.digitalwallet.api.dto.response.UserSessionResponse;
import com.digitalwallet.common.request.RequestIds;
import com.digitalwallet.common.response.ApiResponse;
import com.digitalwallet.domain.entity.User;
import com.digitalwallet.domain.enums.AuditAction;
import com.digitalwallet.domain.enums.AuditActorType;
import com.digitalwallet.security.CurrentUser;
import com.digitalwallet.service.AdminUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<AdminUserResponse>>> listUsers(
            @RequestParam(required = false) String email,
            Pageable pageable,
            HttpServletRequest httpRequest) {
        UUID requestId = RequestIds.getUuid(httpRequest);
        return ResponseEntity.ok(ApiResponse.success(
                requestId.toString(), Instant.now(), adminUserService.listUsers(email, pageable)));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<AdminUserResponse>> getUser(
            @PathVariable UUID userId,
            HttpServletRequest httpRequest) {
        UUID requestId = RequestIds.getUuid(httpRequest);
        return ResponseEntity.ok(ApiResponse.success(
                requestId.toString(), Instant.now(), adminUserService.getUser(userId)));
    }

    @PatchMapping("/users/{userId}/role")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateRole(
            @CurrentUser User admin,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRoleRequest request,
            HttpServletRequest httpRequest) {
        UUID requestId = RequestIds.getUuid(httpRequest);
        AdminUserResponse response = adminUserService.updateRole(admin, userId, request.getRole(), httpRequest, requestId);
        return ResponseEntity.ok(ApiResponse.success(requestId.toString(), Instant.now(), response));
    }

    @PatchMapping("/users/{userId}/status")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateStatus(
            @CurrentUser User admin,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserStatusRequest request,
            HttpServletRequest httpRequest) {
        UUID requestId = RequestIds.getUuid(httpRequest);
        AdminUserResponse response = adminUserService.updateStatus(admin, userId, request.getActive(), httpRequest, requestId);
        return ResponseEntity.ok(ApiResponse.success(requestId.toString(), Instant.now(), response));
    }

    @GetMapping("/users/{userId}/sessions")
    public ResponseEntity<ApiResponse<List<UserSessionResponse>>> listUserSessions(
            @PathVariable UUID userId,
            HttpServletRequest httpRequest) {
        UUID requestId = RequestIds.getUuid(httpRequest);
        return ResponseEntity.ok(ApiResponse.success(
                requestId.toString(), Instant.now(), adminUserService.listUserSessions(userId)));
    }

    @DeleteMapping("/users/{userId}/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> revokeUserSession(
            @CurrentUser User admin,
            @PathVariable UUID userId,
            @PathVariable UUID sessionId,
            HttpServletRequest httpRequest) {
        UUID requestId = RequestIds.getUuid(httpRequest);
        adminUserService.revokeUserSession(admin, userId, sessionId, httpRequest, requestId);
        return ResponseEntity.ok(ApiResponse.success(requestId.toString(), Instant.now(), null));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> listAuditLogs(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) AuditActorType actorType,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            Pageable pageable,
            HttpServletRequest httpRequest) {
        UUID requestId = RequestIds.getUuid(httpRequest);
        return ResponseEntity.ok(ApiResponse.success(
                requestId.toString(), Instant.now(),
                adminUserService.listAuditLogs(userId, action, actorType, from, to, pageable)));
    }

    @GetMapping("/audit-logs/export")
    public void exportAuditLogs(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=audit-logs.csv");
        adminUserService.exportAuditLogsCsv(from, to, response.getWriter());
    }
}