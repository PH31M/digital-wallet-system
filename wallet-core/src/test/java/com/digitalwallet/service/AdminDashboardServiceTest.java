package com.digitalwallet.service;

import com.digitalwallet.api.dto.response.AdminDashboardOverviewResponse;
import com.digitalwallet.api.dto.response.AdminDashboardVolumeResponse;
import com.digitalwallet.domain.enums.FraudReviewStatus;
import com.digitalwallet.domain.enums.TransactionStatus;
import com.digitalwallet.domain.repository.AdminDashboardRepository;
import com.digitalwallet.domain.repository.FraudAssessmentRepository;
import com.digitalwallet.domain.repository.UserRepository;
import com.digitalwallet.domain.repository.WalletRepository;
import com.digitalwallet.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private AdminDashboardRepository dashboardRepository;

    @Mock
    private FraudAssessmentRepository fraudAssessmentRepository;

    @Test
    void getOverview_aggregatesCountsAndRoundsFailRate() {
        when(userRepository.count()).thenReturn(120L);
        when(userRepository.countByIsActiveTrue()).thenReturn(110L);
        when(walletRepository.count()).thenReturn(120L);
        when(walletRepository.sumAllBalances()).thenReturn(new BigDecimal("458200000.00"));
        when(dashboardRepository.countByStatus()).thenReturn(List.of(
                statusCount(TransactionStatus.COMPLETED, 98L),
                statusCount(TransactionStatus.FAILED, 2L)));
        when(fraudAssessmentRepository.countByReviewStatus(FraudReviewStatus.PENDING_REVIEW)).thenReturn(7L);

        AdminDashboardOverviewResponse response = service().getOverview();

        assertThat(response.totalUsers()).isEqualTo(120L);
        assertThat(response.activeUsers()).isEqualTo(110L);
        assertThat(response.totalWallets()).isEqualTo(120L);
        assertThat(response.totalWalletBalance()).isEqualByComparingTo("458200000.00");
        assertThat(response.transactionCountByStatus())
                .containsEntry("COMPLETED", 98L)
                .containsEntry("FAILED", 2L);
        assertThat(response.fraudPendingReviewCount()).isEqualTo(7L);
        assertThat(response.failRatePercent()).isEqualTo(2.0);
        assertThat(response.generatedAt()).isNotNull();
    }

    @Test
    void getOverview_withNoTransactionsReturnsZeroFailRate() {
        when(walletRepository.sumAllBalances()).thenReturn(null);
        when(dashboardRepository.countByStatus()).thenReturn(List.of());
        when(fraudAssessmentRepository.countByReviewStatus(FraudReviewStatus.PENDING_REVIEW)).thenReturn(0L);

        AdminDashboardOverviewResponse response = service().getOverview();

        assertThat(response.totalWalletBalance()).isZero();
        assertThat(response.failRatePercent()).isZero();
        assertThat(Double.isNaN(response.failRatePercent())).isFalse();
    }

    @Test
    void getDailyVolume_mapsPostgresRowsToResponse() {
        LocalDate date = LocalDate.of(2026, 8, 19);
        when(dashboardRepository.dailyVolumeRaw(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(
                        new Object[] { Date.valueOf(date), new BigDecimal("12500000.00"), 84L },
                        new Object[] { date, new BigDecimal("9800000.00"), 61L }));

        AdminDashboardVolumeResponse response = service().getDailyVolume(7);

        assertThat(response.days()).isEqualTo(7);
        assertThat(response.series()).containsExactly(
                new com.digitalwallet.api.dto.response.DailyVolumePoint(date, new BigDecimal("12500000.00"), 84L),
                new com.digitalwallet.api.dto.response.DailyVolumePoint(date, new BigDecimal("9800000.00"), 61L));
    }

    @Test
    void getDailyVolume_outOfRangeDaysThrowsValidationError() {
        assertThatThrownBy(() -> service().getDailyVolume(91))
                .isInstanceOf(BusinessException.class);
    }

    private AdminDashboardService service() {
        return new AdminDashboardService(userRepository, walletRepository, dashboardRepository,
                fraudAssessmentRepository);
    }

    private AdminDashboardRepository.StatusCountProjection statusCount(TransactionStatus status, long count) {
        return new AdminDashboardRepository.StatusCountProjection() {
            @Override
            public TransactionStatus getStatus() {
                return status;
            }

            @Override
            public Long getTransactionCount() {
                return count;
            }
        };
    }
}