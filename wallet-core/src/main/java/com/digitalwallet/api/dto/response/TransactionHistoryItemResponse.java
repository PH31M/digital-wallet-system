package com.digitalwallet.api.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
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