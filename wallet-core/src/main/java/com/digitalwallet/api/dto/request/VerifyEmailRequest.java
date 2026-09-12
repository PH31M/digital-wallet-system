package com.digitalwallet.api.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public class VerifyEmailRequest {

    @JsonProperty("user_id")
    @NotNull(message = "User id is required")
    private UUID userId;

    @NotBlank(message = "OTP is required")
    @Pattern(regexp = "\\d{6}", message = "OTP must be exactly 6 digits")
    private String otp;

    public VerifyEmailRequest() {
    }

    public VerifyEmailRequest(UUID userId, String otp) {
        this.userId = userId;
        this.otp = otp;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }
}
