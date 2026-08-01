package com.digitalwallet.api.dto.response;

import com.digitalwallet.domain.entity.AuditLog;
import com.digitalwallet.domain.enums.AuditAction;
import com.digitalwallet.domain.enums.AuditActorType;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public class AuditLogResponse {

    private final UUID id;

    @JsonProperty("created_at")
    private final Instant createdAt;

    @JsonProperty("actor_id")
    private final UUID actorId;

    @JsonProperty("actor_type")
    private final AuditActorType actorType;

    private final AuditAction action;

    @JsonProperty("resource_type")
    private final String resourceType;

    @JsonProperty("resource_id")
    private final UUID resourceId;

    @JsonProperty("ip_address")
    private final String ipAddress;

    @JsonProperty("user_agent")
    private final String userAgent;

    @JsonProperty("request_id")
    private final UUID requestId;

    public AuditLogResponse(AuditLog auditLog) {
        this.id = auditLog.getId();
        this.createdAt = auditLog.getCreatedAt();
        this.actorId = auditLog.getActor() == null ? null : auditLog.getActor().getId();
        this.actorType = auditLog.getActorType();
        this.action = auditLog.getAction();
        this.resourceType = auditLog.getResourceType();
        this.resourceId = auditLog.getResourceId();
        this.ipAddress = auditLog.getIpAddress();
        this.userAgent = auditLog.getUserAgent();
        this.requestId = auditLog.getRequestId();
    }

    public UUID getId() { return id; }

    public Instant getCreatedAt() { return createdAt; }

    public UUID getActorId() { return actorId; }

    public AuditActorType getActorType() { return actorType; }

    public AuditAction getAction() { return action; }

    public String getResourceType() { return resourceType; }

    public UUID getResourceId() { return resourceId; }

    public String getIpAddress() { return ipAddress; }

    public String getUserAgent() { return userAgent; }

    public UUID getRequestId() { return requestId; }
}