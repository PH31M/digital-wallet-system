package com.digitalwallet.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionHistoryItemResponse(
        UUID id,
        UUID transactionId,
        UUID walletId,
        String type,
        String direction,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String status,
        String description,
        Instant createdAt) {
}