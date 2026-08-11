package com.digitalwallet.exception;

import org.springframework.http.HttpStatus;

/**
 * Centralized application error codes.
 */
public enum ErrorCode {

    // Auth
    EMAIL_ALREADY_EXISTS("EMAIL_ALREADY_EXISTS", HttpStatus.BAD_REQUEST, "Email already exists"),
    WEAK_PASSWORD("WEAK_PASSWORD", HttpStatus.BAD_REQUEST, "Password not strong"),
    ACCOUNT_LOCKED("ACCOUNT_LOCKED", HttpStatus.FORBIDDEN, "Account is locked"),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "Invalid email or password"),
    INVALID_CURRENT_PASSWORD("INVALID_CURRENT_PASSWORD", HttpStatus.BAD_REQUEST, "Current password is incorrect"),
    UNAUTHORIZED("UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "Authentication is required"),
    RATE_LIMITED("RATE_LIMITED", HttpStatus.TOO_MANY_REQUESTS, "Too many requests"),
    RATE_LIMIT_UNAVAILABLE("RATE_LIMIT_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE,
            "Rate limit service is temporarily unavailable"),

    // OTP
    OTP_EXPIRED("OTP_EXPIRED", HttpStatus.BAD_REQUEST, "OTP has expired or is invalid"),
    OTP_MAX_ATTEMPTS("OTP_MAX_ATTEMPTS", HttpStatus.BAD_REQUEST, "Maximum number of OTP attempts exceeded"),
    RESEND_RATE_LIMITED("RESEND_RATE_LIMITED", HttpStatus.BAD_REQUEST,
            "Maximum number of verification email resend requests exceeded"),
    INVALID_VERIFICATION_REQUEST("INVALID_VERIFICATION_REQUEST", HttpStatus.BAD_REQUEST,
            "Verification request is invalid"),
    EMAIL_ALREADY_VERIFIED("EMAIL_ALREADY_VERIFIED", HttpStatus.BAD_REQUEST, "Email is already verified"),

    // Token
    TOKEN_EXPIRED("TOKEN_EXPIRED", HttpStatus.UNAUTHORIZED, "Token has expired or is invalid"),
    TOKEN_REUSE_DETECTED("TOKEN_REUSE_DETECTED", HttpStatus.UNAUTHORIZED, "Token reuse was detected"),
    TOKEN_BLACKLIST_UNAVAILABLE("TOKEN_BLACKLIST_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE,
            "Token validation service is temporarily unavailable"),

    // Generic
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND, "Resource not found"),
    VALIDATION_FAILED("VALIDATION_FAILED", HttpStatus.BAD_REQUEST, "Invalid request data"),
    ACCESS_DENIED("ACCESS_DENIED", HttpStatus.FORBIDDEN, "Access denied"),
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", HttpStatus.INTERNAL_SERVER_ERROR,
            "Unexpected internal server error"),

    BUSINESS_RULE_VIOLATION("BUSINESS_RULE_VIOLATION", HttpStatus.BAD_REQUEST, "Business rule violated"),
    FRAUD_REVIEW_NOT_PENDING("FRAUD_REVIEW_NOT_PENDING", HttpStatus.CONFLICT,
            "Fraud assessment is not pending review"),
    FRAUD_BLOCKED("FRAUD_BLOCKED", HttpStatus.BAD_REQUEST, "Transaction blocked by fraud rules"),
    INSUFFICIENT_BALANCE("INSUFFICIENT_BALANCE", HttpStatus.BAD_REQUEST, "Insufficient balance"),
    DAILY_LIMIT_EXCEEDED("DAILY_LIMIT_EXCEEDED", HttpStatus.BAD_REQUEST, "Daily transaction limit exceeded"),
    WALLET_FROZEN("WALLET_FROZEN", HttpStatus.BAD_REQUEST, "Wallet is frozen");

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(String code, HttpStatus httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
