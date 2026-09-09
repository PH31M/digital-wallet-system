package com.digitalwallet.domain.enums;

/**
 * Transaction lifecycle statuses.
 */
public enum TransactionStatus {
    PENDING,
    PROCESSING,
    PENDING_REVIEW,
    PENDING_OTP_CONFIRMATION,
    COMPLETED,
    FAILED
}
