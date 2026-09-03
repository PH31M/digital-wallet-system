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
import com.digitalwallet.domain.repository.UserRepository;
import com.digitalwallet.domain.repository.UserSessionRepository;
import com.digitalwallet.exception.BusinessException;
import com.digitalwallet.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSessionRepository userSessionRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private AuditService auditService;

    @Test
    void listUsers_noEmailFilter_delegatesToFindAll() {
        AdminUserService service = service();
        when(userRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

        service.listUsers(null, PageRequest.of(0, 10));

        verify(userRepository).findAll(any(Pageable.class));
        verify(userRepository, never()).findByEmailContainingIgnoreCase(any(), any());
    }

    @Test
    void listUsers_withEmailFilter_delegatesToFindByEmailContainingTrimmed() {
        AdminUserService service = service();
        when(userRepository.findByEmailContainingIgnoreCase(eq("test@example.com"), any(Pageable.class)))
                .thenReturn(Page.empty());

        service.listUsers("  test@example.com  ", PageRequest.of(0, 10));

        verify(userRepository).findByEmailContainingIgnoreCase(eq("test@example.com"), any(Pageable.class));
        verify(userRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void listUsers_disallowedSortField_fallsBackToCreatedAtDesc() {
        AdminUserService service = service();
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(userRepository.findAll(pageableCaptor.capture())).thenReturn(Page.empty());

        Pageable requested = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "passwordHash"));
        service.listUsers(null, requested);

        assertThat(pageableCaptor.getValue().getSort())
                .isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Test
    void listUsers_allowedSortField_isPreserved() {
        AdminUserService service = service();
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(userRepository.findAll(pageableCaptor.capture())).thenReturn(Page.empty());

        Pageable requested = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "email"));
        service.listUsers(null, requested);

        assertThat(pageableCaptor.getValue().getSort())
                .isEqualTo(Sort.by(Sort.Direction.ASC, "email"));
    }

    @Test
    void getUser_found_returnsResponse() {
        AdminUserService service = service();
        User user = user("user@example.com", UserRole.USER);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        AdminUserResponse response = service.getUser(user.getId());

        assertThat(response.getEmail()).isEqualTo("user@example.com");
    }

    @Test
    void getUser_notFound_throwsResourceNotFound() {
        AdminUserService service = service();
        UUID missingId = UUID.randomUUID();
        when(userRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getUser(missingId))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void updateRole_success_incrementsTokenVersionRevokesSessionsAndAudits() {
        AdminUserService service = service();
        User admin = user("admin@example.com", UserRole.ADMIN);
        User target = user("user@example.com", UserRole.USER);
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(userRepository.save(target)).thenReturn(target);

        AdminUserResponse response = service.updateRole(admin, target.getId(), UserRole.ADMIN, null, UUID.randomUUID());

        assertThat(target.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(target.getTokenVersion()).isEqualTo(1);
        assertThat(response.getRole()).isEqualTo(UserRole.ADMIN);
        verify(userSessionRepository).revokeActiveByUserId(eq(target.getId()), any(Instant.class));
        verify(auditService).log(eq(admin), eq(AuditActorType.ADMIN), eq(AuditAction.USER_ROLE_CHANGED),
                eq("USER"), eq(target.getId()), any(), any(), any());
    }

    @Test
    void updateRole_userNotFound_throwsAndDoesNotAudit() {
        AdminUserService service = service();
        User admin = user("admin@example.com", UserRole.ADMIN);
        UUID missingId = UUID.randomUUID();
        when(userRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateRole(admin, missingId, UserRole.ADMIN, null, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(auditService);
    }

    @Test
    void updateStatus_deactivate_revokesSessions() {
        AdminUserService service = service();
        User admin = user("admin@example.com", UserRole.ADMIN);
        User target = user("user@example.com", UserRole.USER);
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(userRepository.save(target)).thenReturn(target);

        service.updateStatus(admin, target.getId(), false, null, UUID.randomUUID());

        assertThat(target.getIsActive()).isFalse();
        verify(userSessionRepository).revokeActiveByUserId(eq(target.getId()), any(Instant.class));
        verify(auditService).log(eq(admin), eq(AuditActorType.ADMIN), eq(AuditAction.USER_STATUS_CHANGED),
                eq("USER"), eq(target.getId()), any(), any(), any());
    }

    @Test
    void updateStatus_activate_doesNotRevokeSessions() {
        AdminUserService service = service();
        User admin = user("admin@example.com", UserRole.ADMIN);
        User target = user("user@example.com", UserRole.USER);
        target.setIsActive(false);
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(userRepository.save(target)).thenReturn(target);

        service.updateStatus(admin, target.getId(), true, null, UUID.randomUUID());

        assertThat(target.getIsActive()).isTrue();
        verify(userSessionRepository, never()).revokeActiveByUserId(any(), any());
    }

    @Test
    void listAuditLogs_noUserId_delegatesToFindAllWithSpecification() {
        AdminUserService service = service();
        Page<AuditLog> page = new PageImpl<>(List.of());
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<AuditLogResponse> result = service.listAuditLogs(
                null, null, null, null, null, PageRequest.of(0, 10));

        assertThat(result).isEmpty();
        verify(auditLogRepository).findAll(any(Specification.class), any(Pageable.class));
        verify(auditLogRepository, never()).findByResourceTypeAndResourceId(any(), any(), any());
    }

    @Test
    void listAuditLogs_withUserId_delegatesToFindByResourceTypeAndResourceId() {
        AdminUserService service = service();
        UUID userId = UUID.randomUUID();
        Page<AuditLog> page = new PageImpl<>(List.of());
        when(auditLogRepository.findByResourceTypeAndResourceId(eq("USER"), eq(userId), any(Pageable.class)))
                .thenReturn(page);

        service.listAuditLogs(userId, null, null, null, null, PageRequest.of(0, 10));

        verify(auditLogRepository).findByResourceTypeAndResourceId(eq("USER"), eq(userId), any(Pageable.class));
        verify(auditLogRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void listAuditLogs_withActionFilter_passesSpecificationToRepository() {
        AdminUserService service = service();
        ArgumentCaptor<Specification<AuditLog>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        Page<AuditLog> page = new PageImpl<>(List.of());
        when(auditLogRepository.findAll(specCaptor.capture(), any(Pageable.class))).thenReturn(page);

        service.listAuditLogs(null, AuditAction.USER_LOGIN_FAILED, AuditActorType.USER,
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-31T23:59:59Z"),
                PageRequest.of(0, 10));

        assertThat(specCaptor.getValue()).isNotNull();
        verify(auditLogRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void listAuditLogs_userIdTakesPrecedenceOverOtherFilters() {
        AdminUserService service = service();
        UUID userId = UUID.randomUUID();
        Page<AuditLog> page = new PageImpl<>(List.of());
        when(auditLogRepository.findByResourceTypeAndResourceId(eq("USER"), eq(userId), any(Pageable.class)))
                .thenReturn(page);

        service.listAuditLogs(userId, AuditAction.USER_LOGIN_FAILED, AuditActorType.USER,
                Instant.now(), Instant.now(), PageRequest.of(0, 10));

        verify(auditLogRepository).findByResourceTypeAndResourceId(eq("USER"), eq(userId), any(Pageable.class));
        verify(auditLogRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void listUserSessions_userExists_returnsActiveSessions() {
        AdminUserService service = service();
        User target = user("user@example.com", UserRole.USER);
        UserSession session = UserSession.create(target, "jti-1", "Chrome", "1.2.3.4", "UA",
                Instant.now().plusSeconds(3600));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(userSessionRepository.findActiveByUserId(eq(target.getId()), any(Instant.class)))
                .thenReturn(List.of(session));

        List<UserSessionResponse> sessions = service.listUserSessions(target.getId());

        assertThat(sessions).hasSize(1);
        assertThat(sessions.get(0).getDeviceName()).isEqualTo("Chrome");
    }

    @Test
    void listUserSessions_userNotFound_throwsResourceNotFound() {
        AdminUserService service = service();
        UUID missingId = UUID.randomUUID();
        when(userRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listUserSessions(missingId))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void revokeUserSession_belongsToUser_revokesAndAudits() {
        AdminUserService service = service();
        User admin = user("admin@example.com", UserRole.ADMIN);
        User target = user("user@example.com", UserRole.USER);
        UserSession session = UserSession.create(target, "jti-1", "Chrome", "1.2.3.4", "UA",
                Instant.now().plusSeconds(3600));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(userSessionRepository.findById(session.getId())).thenReturn(Optional.of(session));

        service.revokeUserSession(admin, target.getId(), session.getId(), null, UUID.randomUUID());

        assertThat(session.getRevokedAt()).isNotNull();
        verify(userSessionRepository).save(session);
        verify(auditService).log(eq(admin), eq(AuditActorType.ADMIN), eq(AuditAction.SESSION_REVOKED),
                eq("USER"), eq(target.getId()), any(), any(), any());
    }

    @Test
    void revokeUserSession_belongsToDifferentUser_throwsResourceNotFound() {
        AdminUserService service = service();
        User admin = user("admin@example.com", UserRole.ADMIN);
        User target = user("user@example.com", UserRole.USER);
        User otherUser = user("other@example.com", UserRole.USER);
        UserSession session = UserSession.create(otherUser, "jti-1", "Chrome", "1.2.3.4", "UA",
                Instant.now().plusSeconds(3600));
        when(userRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(userSessionRepository.findById(session.getId())).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.revokeUserSession(admin, target.getId(), session.getId(), null, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);

        verify(userSessionRepository, never()).save(any(UserSession.class));
        verifyNoInteractions(auditService);
    }

    private AdminUserService service() {
        return new AdminUserService(userRepository, userSessionRepository, auditLogRepository, auditService);
    }

    private User user(String email, UserRole role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setFullName("Test User");
        user.setRole(role);
        user.setIsActive(true);
        user.setTokenVersion(0);
        return user;
    }
}
