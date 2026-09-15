package com.digitalwallet.api.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record DailyVolumePoint(
        LocalDate date,
        BigDecimal totalAmount,
        long transactionCount) {
}