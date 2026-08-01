package com.digitalwallet.api.dto.response;

import com.digitalwallet.domain.entity.User;
import com.digitalwallet.domain.enums.UserRole;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public class AdminUserResponse {

    private final UUID id;
    private final String email;

    @JsonProperty("full_name")
    private final String fullName;

    @JsonProperty("phone_number")
    private final String phoneNumber;

    private final UserRole role;

    @JsonProperty("is_active")
    private final Boolean active;

    @JsonProperty("mfa_enabled")
    private final Boolean mfaEnabled;

    @JsonProperty("email_verified_at")
    private final Instant emailVerifiedAt;

    @JsonProperty("locked_until")
    private final Instant lockedUntil;

    @JsonProperty("last_login_at")
    private final Instant lastLoginAt;

    public AdminUserResponse(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.fullName = user.getFullName();
        this.phoneNumber = user.getPhoneNumber();
        this.role = user.getRole();
        this.active = user.getIsActive();
        this.mfaEnabled = user.getMfaEnabled();
        this.emailVerifiedAt = user.getEmailVerifiedAt();
        this.lockedUntil = user.getLockedUntil();
        this.lastLoginAt = user.getLastLoginAt();
    }

    public UUID getId() { return id; }

    public String getEmail() { return email; }

    public String getFullName() { return fullName; }

    public String getPhoneNumber() { return phoneNumber; }

    public UserRole getRole() { return role; }

    public Boolean getActive() { return active; }

    public Boolean getMfaEnabled() { return mfaEnabled; }

    public Instant getEmailVerifiedAt() { return emailVerifiedAt; }

    public Instant getLockedUntil() { return lockedUntil; }

    public Instant getLastLoginAt() { return lastLoginAt; }
}