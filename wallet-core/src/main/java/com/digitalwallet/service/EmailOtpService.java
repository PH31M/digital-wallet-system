package com.digitalwallet.service;

import com.digitalwallet.domain.entity.User;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailOtpService {

    private final OtpService otpService;
    private final EmailService emailService;

    public EmailOtpService(OtpService otpService, EmailService emailService) {
        this.otpService = otpService;
        this.emailService = emailService;
    }

    @Async("asyncExecutor")
    public void sendRegistrationOtp(User user) {
        String otp = otpService.generateOtp();
        otpService.saveRegistrationOtp(user.getId(), otp);
        emailService.sendVerificationEmail(user.getEmail(), otp);
    }

    @Async("asyncExecutor")
    public void sendPasswordResetOtp(User user) {
        String otp = otpService.generateOtp();
        otpService.savePasswordResetOtp(user.getId(), otp);
        emailService.sendPasswordResetEmail(user.getEmail(), otp);
    }

    @Async("asyncExecutor")
    public void sendMfaOtp(User user) {
        String otp = otpService.generateOtp();
        otpService.saveMfaOtp(user.getId(), otp);
        emailService.sendMfaEmail(user.getEmail(), otp);
    }
}