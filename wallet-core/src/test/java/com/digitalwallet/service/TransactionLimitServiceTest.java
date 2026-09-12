package com.digitalwallet.service;

import com.digitalwallet.domain.entity.Wallet;
import com.digitalwallet.domain.repository.TransactionRepository;
import com.digitalwallet.exception.DailyLimitExceededException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionLimitServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Test
    void assertWithinDailyLimit_allowsWhenLimitHasHeadroom() {
        Wallet wallet = wallet();
        TransactionLimitService service = service("2026-08-01T10:15:30Z", "1000.00");
        when(transactionRepository.sumMoneyMovementForWallet(eq(wallet.getId()), any(), any()))
                .thenReturn(new BigDecimal("600.00"));

        assertThatCode(() -> service.assertWithinDailyLimit(wallet, new BigDecimal("300.00")))
                .doesNotThrowAnyException();
    }

    @Test
    void assertWithinDailyLimit_rejectsWhenAmountWouldExceedLimit() {
        Wallet wallet = wallet();
        TransactionLimitService service = service("2026-08-01T10:15:30Z", "1000.00");
        when(transactionRepository.sumMoneyMovementForWallet(eq(wallet.getId()), any(), any()))
                .thenReturn(new BigDecimal("900.00"));

        assertThatThrownBy(() -> service.assertWithinDailyLimit(wallet, new BigDecimal("200.00")))
                .isInstanceOf(DailyLimitExceededException.class);
    }

    @Test
    void assertWithinDailyLimit_queriesCurrentUtcDayWindow() {
        Wallet wallet = wallet();
        TransactionLimitService service = service("2026-08-01T10:15:30Z", "1000.00");
        when(transactionRepository.sumMoneyMovementForWallet(eq(wallet.getId()), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        service.assertWithinDailyLimit(wallet, new BigDecimal("100.00"));

        verify(transactionRepository).sumMoneyMovementForWallet(
                eq(wallet.getId()),
                eq(Instant.parse("2026-08-01T00:00:00Z")),
                eq(Instant.parse("2026-08-02T00:00:00Z")));
    }

    private TransactionLimitService service(String instant, String limit) {
        return new TransactionLimitService(transactionRepository, new BigDecimal(limit), "UTC",
                Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
    }

    private Wallet wallet() {
        Wallet wallet = new Wallet();
        wallet.setId(UUID.randomUUID());
        return wallet;
    }
}