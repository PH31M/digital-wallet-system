package com.digitalwallet.exception;

/**
 * Base custom exception for wallet application errors.
 */
public abstract class DigitalWalletException extends BusinessException {

    protected DigitalWalletException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    protected DigitalWalletException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
