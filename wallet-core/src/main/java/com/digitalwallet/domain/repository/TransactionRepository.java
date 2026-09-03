package com.digitalwallet.domain.repository;

import com.digitalwallet.domain.entity.Transaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Transaction entities.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findBySenderWalletIdOrReceiverWalletId(UUID senderWalletId, UUID receiverWalletId);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Transaction t WHERE t.id = :id")
    Optional<Transaction> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE (t.senderWallet.id = :walletId OR t.receiverWallet.id = :walletId)
              AND t.status IN (com.digitalwallet.domain.enums.TransactionStatus.PROCESSING,
                               com.digitalwallet.domain.enums.TransactionStatus.COMPLETED)
              AND t.createdAt >= :from
              AND t.createdAt < :to
            """)
    BigDecimal sumMoneyMovementForWallet(@Param("walletId") UUID walletId,
            @Param("from") Instant from,
            @Param("to") Instant to);
}
