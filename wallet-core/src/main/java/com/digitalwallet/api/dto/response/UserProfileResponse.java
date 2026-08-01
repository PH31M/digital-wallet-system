package com.digitalwallet.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class UserProfileResponse {

    private final UUID id;
    private final String email;

    @JsonProperty("full_name")
    private final String fullName;

    @JsonProperty("phone_number")
    private final String phoneNumber;

    @JsonProperty("is_verified")
    private final boolean verified;

    public UserProfileResponse(UUID id, String email, String fullName, boolean verified) {
        this(id, email, fullName, null, verified);
    }

    public UserProfileResponse(UUID id, String email, String fullName, String phoneNumber, boolean verified) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.verified = verified;
    }

    public UUID getId() {
        return id;
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

    public boolean isVerified() {
        return verified;
    }
}
