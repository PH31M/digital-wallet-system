package com.digitalwallet.domain.event;

import com.digitalwallet.domain.entity.Transaction;
import com.digitalwallet.domain.entity.User;

import java.util.UUID;

public record TransactionFailedEvent(User user, UUID userPublicId, Transaction transaction, String reason) {

    public static TransactionFailedEvent of(User user, Transaction transaction, String reason) {
        return new TransactionFailedEvent(user, user.getPublicId(), transaction, reason);
    }
}