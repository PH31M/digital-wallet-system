package com.digitalwallet.exception;

/**
 * Thrown when a receiver wallet or user is not found.
 */
public class ReceiverNotFoundException extends DigitalWalletException {

    public ReceiverNotFoundException(String message) {
        super(message);
    }
}
