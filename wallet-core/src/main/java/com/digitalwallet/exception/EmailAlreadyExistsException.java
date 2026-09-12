package com.digitalwallet.exception;

/**
 * Thrown when trying to register an email that already exists.
 */
public class EmailAlreadyExistsException extends BusinessException {

    public EmailAlreadyExistsException(String message) {
        super(ErrorCode.EMAIL_ALREADY_EXISTS, message);
    }
}
