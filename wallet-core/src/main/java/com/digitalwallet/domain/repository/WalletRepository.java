package com.digitalwallet.domain.repository;

import com.digitalwallet.domain.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Wallet entities.
 */
@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.id = :id")
    Optional<Wallet> findByIdForUpdate(@Param("id") UUID id);

    @Query("SELECT w FROM Wallet w WHERE w.user.id = :userId")
    Optional<Wallet> findByUserId(@Param("userId") UUID userId);

    @Query("SELECT w.id FROM Wallet w WHERE w.user.id = :userId")
    List<UUID> findWalletIdsByUserId(@Param("userId") UUID userId);

    @Query("SELECT COALESCE(SUM(w.balance), 0) FROM Wallet w")
    BigDecimal sumAllBalances();
}
