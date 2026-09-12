package com.digitalwallet.api.dto.request;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionHistoryFilter(
        String type,
        String status,
        Instant dateFrom,
        Instant dateTo,
        BigDecimal minAmount,
        BigDecimal maxAmount) {
}