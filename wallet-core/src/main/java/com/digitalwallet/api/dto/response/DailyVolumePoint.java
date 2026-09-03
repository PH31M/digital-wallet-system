package com.digitalwallet.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyVolumePoint(
        LocalDate date,
        BigDecimal totalAmount,
        long transactionCount) {
}