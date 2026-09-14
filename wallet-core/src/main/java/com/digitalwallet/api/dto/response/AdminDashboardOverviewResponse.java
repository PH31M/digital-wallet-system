package com.digitalwallet.api.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AdminDashboardOverviewResponse(
        long totalUsers,
        long activeUsers,
        long totalWallets,
        BigDecimal totalWalletBalance,
        Map<String, Long> transactionCountByStatus,
        long fraudPendingReviewCount,
        double failRatePercent,
        Instant generatedAt) {
}