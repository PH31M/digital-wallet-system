package com.digitalwallet.exception;

/**
 * Base exception for business errors exposed to API clients.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String field;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.field = null;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.field = null;
    }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.field = null;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getField() {
        return field;
    }
}
