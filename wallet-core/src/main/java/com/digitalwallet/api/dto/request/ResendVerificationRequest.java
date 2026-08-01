package com.digitalwallet.api.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class ResendVerificationRequest {

    @JsonProperty("user_id")
    @NotNull(message = "User id is required")
    private UUID userId;

    @NotBlank(message = "Email is required")
    @Email(message = "Email is invalid")
    private String email;

    public ResendVerificationRequest() {
    }

    public ResendVerificationRequest(UUID userId, String email) {
        this.userId = userId;
        this.email = email;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}