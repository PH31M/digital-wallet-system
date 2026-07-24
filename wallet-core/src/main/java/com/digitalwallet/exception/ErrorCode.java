package com.digitalwallet.exception;

import org.springframework.http.HttpStatus;

/**
 * Centralized application error codes.
 */
public enum ErrorCode {

    // Auth
    EMAIL_ALREADY_EXISTS("EMAIL_ALREADY_EXISTS", HttpStatus.CONFLICT, "Email already exists"),
    WEAK_PASSWORD("WEAK_PASSWORD", HttpStatus.BAD_REQUEST, "Password not strong"),
    ACCOUNT_LOCKED("ACCOUNT_LOCKED", HttpStatus.FORBIDDEN, "Account is locked"),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "Email or Password is wrong"),

    // OTP
    OTP_EXPIRED("OTP_EXPIRED", HttpStatus.BAD_REQUEST, "OTP expire"),
    OTP_MAX_ATTEMPTS("OTP_MAX_ATTEMPTS", HttpStatus.BAD_REQUEST, "Maximum number of OTP attempts exceeded."),

    // Token
    TOKEN_EXPIRED("TOKEN_EXPIRED", HttpStatus.UNAUTHORIZED, "Token đã hết hạn"),
    TOKEN_REUSE_DETECTED("TOKEN_REUSE_DETECTED", HttpStatus.UNAUTHORIZED, "Phát hiện token bị tái sử dụng"),

    // Generic
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND, "Không tìm thấy tài nguyên"),
    VALIDATION_FAILED("VALIDATION_FAILED", HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ"),
    ACCESS_DENIED("ACCESS_DENIED", HttpStatus.FORBIDDEN, "Không có quyền truy cập"),
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", HttpStatus.INTERNAL_SERVER_ERROR,
            "Unexpected internal server error"),

    BUSINESS_RULE_VIOLATION("BUSINESS_RULE_VIOLATION", HttpStatus.BAD_REQUEST, "Business rule violated"),
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
