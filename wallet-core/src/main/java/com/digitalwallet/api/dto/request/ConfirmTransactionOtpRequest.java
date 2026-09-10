package com.digitalwallet.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public class ConfirmTransactionOtpRequest {

    @NotBlank(message = "OTP is required")
    private String otp;

    public ConfirmTransactionOtpRequest() {
    }

    public ConfirmTransactionOtpRequest(String otp) {
        this.otp = otp;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }
}
