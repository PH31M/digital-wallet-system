package com.digitalwallet.api.dto.response;

import com.digitalwallet.domain.entity.UserSession;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public class UserSessionResponse {

    private final UUID id;

    @JsonProperty("device_name")
    private final String deviceName;

    @JsonProperty("ip_address")
    private final String ipAddress;

    @JsonProperty("user_agent")
    private final String userAgent;

    @JsonProperty("last_used_at")
    private final Instant lastUsedAt;

    @JsonProperty("expires_at")
    private final Instant expiresAt;

    public UserSessionResponse(UserSession session) {
        this.id = session.getId();
        this.deviceName = session.getDeviceName();
        this.ipAddress = session.getIpAddress();
        this.userAgent = session.getUserAgent();
        this.lastUsedAt = session.getLastUsedAt();
        this.expiresAt = session.getExpiresAt();
    }

    public UUID getId() {
        return id;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}