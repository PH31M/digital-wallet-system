package com.digitalwallet.service;

import com.digitalwallet.api.dto.response.AdminDashboardOverviewResponse;
import com.digitalwallet.api.dto.response.AdminDashboardVolumeResponse;
import com.digitalwallet.api.dto.response.DailyVolumePoint;
import com.digitalwallet.domain.enums.FraudReviewStatus;
import com.digitalwallet.domain.enums.UserRole;
import com.digitalwallet.domain.repository.AdminDashboardRepository;
import com.digitalwallet.domain.repository.FraudAssessmentRepository;
import com.digitalwallet.domain.repository.UserRepository;
import com.digitalwallet.domain.repository.WalletRepository;
import com.digitalwallet.exception.BusinessException;
import com.digitalwallet.exception.ErrorCode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminDashboardService {

    private static final int MIN_VOLUME_DAYS = 1;
    private static final int MAX_VOLUME_DAYS = 90;

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final AdminDashboardRepository dashboardRepository;
    private final FraudAssessmentRepository fraudAssessmentRepository;

    public AdminDashboardService(
            UserRepository userRepository,
            WalletRepository walletRepository,
            AdminDashboardRepository dashboardRepository,
            FraudAssessmentRepository fraudAssessmentRepository) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.dashboardRepository = dashboardRepository;
        this.fraudAssessmentRepository = fraudAssessmentRepository;
    }

    @Cacheable(cacheNames = "admin:dashboard:overview")
    public AdminDashboardOverviewResponse getOverview() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByIsActiveTrue();
        long totalWallets = walletRepository.count();
        BigDecimal totalBalance = valueOrZero(walletRepository.sumAllBalances());

        Map<String, Long> countsByStatus = new LinkedHashMap<>();
        dashboardRepository.countByStatus().forEach(projection ->
                countsByStatus.put(projection.getStatus().name(), projection.getTransactionCount()));

        long totalTransactions = countsByStatus.values().stream()
                .mapToLong(Long::longValue)
                .sum();
        long failedTransactions = countsByStatus.getOrDefault("FAILED", 0L);
        double failRatePercent = calculateFailRatePercent(failedTransactions, totalTransactions);
        long fraudPendingReviewCount = fraudAssessmentRepository
                .countByReviewStatus(FraudReviewStatus.PENDING_REVIEW);

        return new AdminDashboardOverviewResponse(
                totalUsers,
                activeUsers,
                totalWallets,
                totalBalance,
                Map.copyOf(countsByStatus),
                fraudPendingReviewCount,
                failRatePercent,
                Instant.now());
    }

    @Cacheable(cacheNames = "admin:dashboard:volume", key = "#days")
    public AdminDashboardVolumeResponse getDailyVolume(int days) {
        validateDays(days);

        Instant since = Instant.now().minus(days, ChronoUnit.DAYS);
        List<DailyVolumePoint> series = dashboardRepository.dailyVolumeRaw(since).stream()
                .map(this::toDailyVolumePoint)
                .toList();

        return new AdminDashboardVolumeResponse(days, series, Instant.now());
    }

    private double calculateFailRatePercent(long failedTransactions, long totalTransactions) {
        if (totalTransactions == 0) {
            return 0.0;
        }

        return BigDecimal.valueOf(failedTransactions)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalTransactions), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private DailyVolumePoint toDailyVolumePoint(Object[] row) {
        return new DailyVolumePoint(
                toLocalDate(row[0]),
                toBigDecimal(row[1]),
                ((Number) row[2]).longValue());
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate date) {
            return date;
        }
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate();
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate();
        }
        if (value instanceof Instant instant) {
            return instant.atZone(ZoneOffset.UTC).toLocalDate();
        }
        return LocalDate.parse(value.toString());
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal amount) {
            return amount;
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        return new BigDecimal(value.toString());
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void validateDays(int days) {
        if (days < MIN_VOLUME_DAYS || days > MAX_VOLUME_DAYS) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_FAILED,
                    "days must be between " + MIN_VOLUME_DAYS + " and " + MAX_VOLUME_DAYS);
        }
    }
}