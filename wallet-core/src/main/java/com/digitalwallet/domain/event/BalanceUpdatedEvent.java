package com.digitalwallet.domain.event;

import com.digitalwallet.domain.entity.User;
import com.digitalwallet.domain.entity.Wallet;

import java.math.BigDecimal;
import java.util.UUID;

public record BalanceUpdatedEvent(User user, UUID userPublicId, UUID walletId, BigDecimal balance) {

    public static BalanceUpdatedEvent of(Wallet wallet) {
        return new BalanceUpdatedEvent(
                wallet.getUser(),
                wallet.getUser().getPublicId(),
                wallet.getId(),
                wallet.getBalance());
    }
}