package com.digitalwallet.service;

import com.digitalwallet.domain.entity.AuditLog;
import com.digitalwallet.domain.entity.User;
import com.digitalwallet.domain.enums.AuditAction;
import com.digitalwallet.domain.enums.AuditActorType;
import com.digitalwallet.domain.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Coverage for DWS-69/70: append-only AuditLog entity + AuditService.log().
 *
 * These tests intentionally also pin down two behaviors that currently
 * DEVIATE from the ticket description and are worth flagging:
 *  - AuditService.log() is synchronous (no @Async), so a slow/failing audit
 *    write can block or roll back the caller's transaction.
 *  - Callers must pass ipAddress/userAgent explicitly instead of AuditService
 *    capturing them itself via RequestContextHolder.
 * The tests below assert the CURRENT contract; if that contract changes
 * (e.g. @Async is added) these tests should be revisited accordingly.
 */
@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Test
    void log_persistsAuditLogWithAllProvidedFields() {
        AuditService auditService = new AuditService(auditLogRepository);
        User actor = new User();
        actor.setId(UUID.randomUUID());
        UUID resourceId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        auditService.log(actor, AuditActorType.USER, AuditAction.USER_REGISTERED,
                "USER", resourceId, "203.0.113.5", "Mozilla/5.0", requestId);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();

        assertThat(saved.getActor()).isEqualTo(actor);
        assertThat(saved.getActorType()).isEqualTo(AuditActorType.USER);
        assertThat(saved.getAction()).isEqualTo(AuditAction.USER_REGISTERED);
        assertThat(saved.getResourceType()).isEqualTo("USER");
        assertThat(saved.getResourceId()).isEqualTo(resourceId);
        assertThat(saved.getIpAddress()).isEqualTo("203.0.113.5");
        assertThat(saved.getUserAgent()).isEqualTo("Mozilla/5.0");
        assertThat(saved.getRequestId()).isEqualTo(requestId);
    }

    @Test
    void log_allowsNullActorForSystemGeneratedActions() {
        AuditService auditService = new AuditService(auditLogRepository);
        UUID resourceId = UUID.randomUUID();

        auditService.log(null, AuditActorType.SYSTEM, AuditAction.WALLET_FROZEN,
                "WALLET", resourceId, null, null, null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();

        assertThat(saved.getActor()).isNull();
        assertThat(saved.getActorType()).isEqualTo(AuditActorType.SYSTEM);
        assertThat(saved.getIpAddress()).isNull();
        assertThat(saved.getUserAgent()).isNull();
        assertThat(saved.getRequestId()).isNull();
    }

    @Test
    void log_eachDistinctActionProducesItsOwnPersistedRow() {
        AuditService auditService = new AuditService(auditLogRepository);
        User actor = new User();
        actor.setId(UUID.randomUUID());

        auditService.log(actor, AuditActorType.USER, AuditAction.USER_LOGIN_SUCCESS,
                "USER", actor.getId(), "10.0.0.1", "agent", UUID.randomUUID());
        auditService.log(actor, AuditActorType.USER, AuditAction.USER_LOGIN_FAILED,
                "USER", actor.getId(), "10.0.0.1", "agent", UUID.randomUUID());

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(AuditLog::getAction)
                .containsExactly(AuditAction.USER_LOGIN_SUCCESS, AuditAction.USER_LOGIN_FAILED);
    }

    @Test
    void auditLogEntity_hasCreatedAtButNoUpdatedAtField() {
        assertThat(hasDeclaredField(AuditLog.class, "updatedAt")).isFalse();
        assertThat(hasDeclaredField(com.digitalwallet.domain.entity.BaseEntity.class, "createdAt")).isTrue();
    }

    @Test
    void auditLogEntity_isAppendOnly_exposesNoSetterMethods() {
        // AuditLog must never be mutated after creation - guards the "append-only" guarantee
        // called out explicitly in the ticket (no update() method, no setters).
        long setterCount = Arrays.stream(AuditLog.class.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .map(Method::getName)
                .filter(name -> name.startsWith("set"))
                .count();

        assertThat(setterCount).isZero();
    }

    private boolean hasDeclaredField(Class<?> type, String fieldName) {
        return Arrays.stream(type.getDeclaredFields()).anyMatch(f -> f.getName().equals(fieldName));
    }
}
