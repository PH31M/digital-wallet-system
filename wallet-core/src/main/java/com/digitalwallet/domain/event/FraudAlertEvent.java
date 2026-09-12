package com.digitalwallet.domain.event;

import com.digitalwallet.domain.entity.Transaction;
import com.digitalwallet.domain.entity.User;

import java.util.UUID;

public record FraudAlertEvent(User user, UUID userPublicId, Transaction transaction) {

    public static FraudAlertEvent of(User user, Transaction transaction) {
        return new FraudAlertEvent(user, user.getPublicId(), transaction);
    }
}