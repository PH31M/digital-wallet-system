package com.digitalwallet.exception;

/**
 * Base custom exception for wallet application errors.
 */
public class DigitalWalletException extends RuntimeException {

    public DigitalWalletException(String message) {
        super(message);
    }

    public DigitalWalletException(String message, Throwable cause) {
        super(message, cause);
    }
}
