package com.digitalwallet.service;

import com.digitalwallet.domain.entity.AuditLog;
import com.digitalwallet.domain.entity.User;
import com.digitalwallet.domain.enums.AuditAction;
import com.digitalwallet.domain.enums.AuditActorType;
import com.digitalwallet.domain.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(User actor, AuditActorType actorType, AuditAction actionType,
            String resourceType, UUID resourceId, String ipAddress,
            String userAgent, UUID requestId) {
        auditLogRepository.save(AuditLog.of(actor, actorType, actionType, resourceType, resourceId,
                ipAddress, userAgent, requestId));
    }
}
