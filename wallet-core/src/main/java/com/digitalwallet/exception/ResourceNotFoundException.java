package com.digitalwallet.exception;

/**
 * Thrown when a requested resource cannot be found.
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
    }
}
