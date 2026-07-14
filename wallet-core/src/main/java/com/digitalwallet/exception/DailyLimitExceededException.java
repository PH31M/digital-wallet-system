package com.digitalwallet.exception;

/**
 * Thrown when a wallet exceeds a daily transaction limit.
 */
public class DailyLimitExceededException extends DigitalWalletException {

    public DailyLimitExceededException(String message) {
        super(message);
    }
}
