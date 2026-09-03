package com.digitalwallet.domain.event;

import com.digitalwallet.domain.entity.Transaction;
import com.digitalwallet.domain.entity.User;
import com.digitalwallet.domain.enums.NotificationType;

import java.util.List;
import java.util.UUID;

public record TransactionCompletedEvent(Transaction transaction, List<Recipient> recipients) {

    public record Recipient(User user, UUID userPublicId, NotificationType notificationType) {

        public static Recipient of(User user, NotificationType notificationType) {
            return new Recipient(user, user.getPublicId(), notificationType);
        }
    }
}