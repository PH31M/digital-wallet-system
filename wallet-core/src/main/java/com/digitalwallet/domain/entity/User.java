package com.digitalwallet.domain.entity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import com.digitalwallet.domain.enums.UserRole;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User extends BaseAuditableEntity {

    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 15;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId = UUID.randomUUID();

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "phone_number", unique = true)
    private String phoneNumber;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.USER;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "failed_login_attempts", nullable = false)
    private Integer failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "token_version", nullable = false)
    private Integer tokenVersion = 0;

    @Column(name = "mfa_enabled", nullable = false)
    private Boolean mfaEnabled = false;

    public UUID getPublicId() {
        return publicId;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public Instant getEmailVerifiedAt() {
        return emailVerifiedAt;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public Integer getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public Integer getTokenVersion() {
        return tokenVersion;
    }

    public Boolean getMfaEnabled() {
        return mfaEnabled;
    }

    public void setPublicId(UUID publicId) {
        this.publicId = publicId;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public void setEmailVerifiedAt(Instant emailVerifiedAt) {
        this.emailVerifiedAt = emailVerifiedAt;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public void setFailedLoginAttempts(Integer failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public void setLockedUntil(Instant lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public void setTokenVersion(Integer tokenVersion) {
        this.tokenVersion = tokenVersion;
    }

    public void setMfaEnabled(Boolean mfaEnabled) {
        this.mfaEnabled = mfaEnabled;
    }

    public void recordFailedLogin() {
        this.failedLoginAttempts = this.failedLoginAttempts == null ? 1 : this.failedLoginAttempts + 1;
        if (this.failedLoginAttempts >= MAX_FAILED_LOGIN_ATTEMPTS) {
            this.lockedUntil = Instant.now().plus(LOCK_DURATION_MINUTES, ChronoUnit.MINUTES);
        }
    }

    public void recordSuccessfulLogin() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
        this.lastLoginAt = Instant.now();
    }

    public void markAsVerified() {
        if (this.emailVerifiedAt == null) {
            this.emailVerifiedAt = Instant.now();
        }
    }

    public void updateProfile(String fullName, String phoneNumber) {
        if (fullName != null) {
            this.fullName = fullName.trim();
        }
        this.phoneNumber = phoneNumber;
    }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
        incrementTokenVersion();
    }

    public void incrementTokenVersion() {
        this.tokenVersion = this.tokenVersion == null ? 1 : this.tokenVersion + 1;
    }

    @PrePersist
    void ensurePublicId() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
    }

    public boolean isCurrentlyLocked() {
        return lockedUntil != null && lockedUntil.isAfter(Instant.now());
    }

}
