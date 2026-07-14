package com.digitalwallet.exception;

/**
 * Thrown when wallet balance is insufficient.
 */
public class InsufficientBalanceException extends DigitalWalletException {

    public InsufficientBalanceException(String message) {
        super(message);
    }
}
