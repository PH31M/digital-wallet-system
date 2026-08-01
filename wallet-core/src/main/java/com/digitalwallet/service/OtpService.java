package com.digitalwallet.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class OtpService {

    private static final Duration OTP_TTL = Duration.ofMinutes(10);
    private static final Duration RESEND_WINDOW = Duration.ofHours(1);
    private static final int MAX_ATTEMPTS = 3;
    private static final int MAX_RESEND_PER_HOUR = 3;
    private static final int MAX_RESEND_PER_IP_PER_HOUR = 10;
    private static final String REGISTER_PURPOSE = "register";
    private static final String PASSWORD_RESET_PURPOSE = "password-reset";
    private static final String MFA_PURPOSE = "mfa";

    private static final DefaultRedisScript<Long> VERIFY_AND_DELETE_SCRIPT = new DefaultRedisScript<>(
            """
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                redis.call('DEL', KEYS[1])
                redis.call('DEL', KEYS[2])
                return 1
            end
            return 0
            """,
            Long.class);

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String generateOtp() {
        return String.valueOf(100000 + secureRandom.nextInt(900000));
    }

    public void saveOtp(UUID userId, String purpose, String otp) {
        String otpKey = buildOtpKey(userId, purpose);
        String attemptsKey = buildAttemptsKey(userId, purpose);

        redisTemplate.opsForValue().set(otpKey, hash(otp), OTP_TTL);
        redisTemplate.opsForValue().set(attemptsKey, "0", OTP_TTL);
    }

    public boolean verifyOtp(UUID userId, String purpose, String inputOtp) {
        Long verified = redisTemplate.execute(
                VERIFY_AND_DELETE_SCRIPT,
                List.of(buildOtpKey(userId, purpose), buildAttemptsKey(userId, purpose)),
                hash(inputOtp));
        return Long.valueOf(1L).equals(verified);
    }

    public int getAttempts(UUID userId, String purpose) {
        String value = redisTemplate.opsForValue().get(buildAttemptsKey(userId, purpose));
        return value == null ? 0 : Integer.parseInt(value);
    }

    public int incrementAttempts(UUID userId, String purpose) {
        String key = buildAttemptsKey(userId, purpose);
        Long attempts = redisTemplate.opsForValue().increment(key);
        if (Long.valueOf(1L).equals(attempts)) {
            redisTemplate.expire(key, OTP_TTL);
        }
        return attempts != null ? attempts.intValue() : 0;
    }

    public boolean hasExceededMaxAttempts(UUID userId, String purpose) {
        return getAttempts(userId, purpose) >= MAX_ATTEMPTS;
    }

    public boolean tryConsumeResendQuota(UUID userId) {
        String key = "otp:resend:" + userId;
        Long count = redisTemplate.opsForValue().increment(key);
        if (Long.valueOf(1L).equals(count)) {
            redisTemplate.expire(key, RESEND_WINDOW);
        }
        return count != null && count <= MAX_RESEND_PER_HOUR;
    }

    public boolean tryConsumeResendIpQuota(String ipAddress) {
        String key = "otp:resend:ip:" + hash(ipAddress == null ? "unknown" : ipAddress);
        Long count = redisTemplate.opsForValue().increment(key);
        if (Long.valueOf(1L).equals(count)) {
            redisTemplate.expire(key, RESEND_WINDOW);
        }
        return count != null && count <= MAX_RESEND_PER_IP_PER_HOUR;
    }

    public void saveRegistrationOtp(UUID userId, String otp) {
        saveOtp(userId, REGISTER_PURPOSE, otp);
    }

    public boolean verifyRegistrationOtp(UUID userId, String otp) {
        return verifyOtp(userId, REGISTER_PURPOSE, otp);
    }

    public boolean hasExceededRegistrationAttempts(UUID userId) {
        return hasExceededMaxAttempts(userId, REGISTER_PURPOSE);
    }

    public int incrementRegistrationAttempts(UUID userId) {
        return incrementAttempts(userId, REGISTER_PURPOSE);
    }

    public void savePasswordResetOtp(UUID userId, String otp) {
        saveOtp(userId, PASSWORD_RESET_PURPOSE, otp);
    }

    public boolean verifyPasswordResetOtp(UUID userId, String otp) {
        return verifyOtp(userId, PASSWORD_RESET_PURPOSE, otp);
    }

    public boolean hasExceededPasswordResetAttempts(UUID userId) {
        return hasExceededMaxAttempts(userId, PASSWORD_RESET_PURPOSE);
    }

    public int incrementPasswordResetAttempts(UUID userId) {
        return incrementAttempts(userId, PASSWORD_RESET_PURPOSE);
    }

    public void saveMfaOtp(UUID userId, String otp) {
        saveOtp(userId, MFA_PURPOSE, otp);
    }

    public boolean verifyMfaOtp(UUID userId, String otp) {
        return verifyOtp(userId, MFA_PURPOSE, otp);
    }

    public boolean hasExceededMfaAttempts(UUID userId) {
        return hasExceededMaxAttempts(userId, MFA_PURPOSE);
    }

    public int incrementMfaAttempts(UUID userId) {
        return incrementAttempts(userId, MFA_PURPOSE);
    }

    private String buildOtpKey(UUID userId, String purpose) {
        return "otp:%s:%s".formatted(purpose, userId);
    }

    private String buildAttemptsKey(UUID userId, String purpose) {
        return "otp:%s:attempts:%s".formatted(purpose, userId);
    }

    private String hash(String otp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(otp.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
