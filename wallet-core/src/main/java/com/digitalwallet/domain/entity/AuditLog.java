package com.digitalwallet.domain.entity;

import com.digitalwallet.domain.enums.AuditAction;
import com.digitalwallet.domain.enums.AuditActorType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLog extends BaseEntity { // id + createdAt kế thừa từ đây, KHÔNG khai báo lại

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id") // null nếu action tự động (SYSTEM)
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 20)
    private AuditActorType actorType;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50)
    private AuditAction actionType;

    @Column(name = "resource_type", nullable = false)
    private String resourceType;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;

    @Column(name = "request_id")
    private UUID requestId;

    protected AuditLog() {
    }

    private AuditLog(User actor, AuditActorType actorType, AuditAction actionType,
            String resourceType, UUID resourceId, String ipAddress,
            String userAgent, UUID requestId) {
        this.actor = actor;
        this.actorType = actorType;
        this.actionType = actionType;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.requestId = requestId;
        // createdAt tự set trong BaseEntity (@PrePersist)
    }

    // Chỉ getter — KHÔNG setter nào, đảm bảo append-only
    public static AuditLog of(User actor, AuditActorType actorType, AuditAction actionType,
            String resourceType, UUID resourceId, String ipAddress,
            String userAgent, UUID requestId) {
        return new AuditLog(actor, actorType, actionType, resourceType, resourceId,
                ipAddress, userAgent, requestId);
    }

    public User getActor() {
        return actor;
    }

    public AuditActorType getActorType() {
        return actorType;
    }

    public AuditAction getAction() {
        return actionType;
    }

    public String getResourceType() {
        return resourceType;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public UUID getRequestId() {
        return requestId;
    }
}