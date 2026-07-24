package com.digitalwallet.domain.repository;

import com.digitalwallet.domain.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Transaction entities.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findBySenderWalletIdOrReceiverWalletId(UUID senderWalletId, UUID receiverWalletId);
}
