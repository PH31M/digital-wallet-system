package com.digitalwallet.service;

import com.digitalwallet.domain.entity.Wallet;
import com.digitalwallet.domain.repository.TransactionRepository;
import com.digitalwallet.exception.DailyLimitExceededException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class TransactionLimitService {

    private final TransactionRepository transactionRepository;
    private final BigDecimal dailyLimit;
    private final ZoneId zoneId;
    private final Clock clock;

    public TransactionLimitService(TransactionRepository transactionRepository,
            @Value("${wallet.limits.daily-money-movement:50000000}") BigDecimal dailyLimit,
            @Value("${wallet.limits.zone:UTC}") String zoneId,
            Clock clock) {
        this.transactionRepository = transactionRepository;
        this.dailyLimit = dailyLimit;
        this.zoneId = ZoneId.of(zoneId);
        this.clock = clock;
    }

    public void assertWithinDailyLimit(Wallet wallet, BigDecimal amount) {
        LocalDate today = LocalDate.now(clock.withZone(zoneId));
        Instant from = today.atStartOfDay(zoneId).toInstant();
        Instant to = today.plusDays(1).atStartOfDay(zoneId).toInstant();
        BigDecimal usedToday = transactionRepository.sumMoneyMovementForWallet(wallet.getId(), from, to);
        BigDecimal normalizedUsedToday = usedToday == null ? BigDecimal.ZERO : usedToday;
        if (normalizedUsedToday.add(amount).compareTo(dailyLimit) > 0) {
            throw new DailyLimitExceededException("Daily transaction limit exceeded");
        }
    }
}