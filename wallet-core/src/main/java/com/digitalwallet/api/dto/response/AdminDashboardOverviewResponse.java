package com.digitalwallet.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

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