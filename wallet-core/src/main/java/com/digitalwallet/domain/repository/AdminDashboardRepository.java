package com.digitalwallet.domain.repository;

import com.digitalwallet.domain.entity.Transaction;
import com.digitalwallet.domain.enums.TransactionStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@org.springframework.stereotype.Repository
public interface AdminDashboardRepository extends Repository<Transaction, UUID> {

    @Query("""
            SELECT t.status AS status, COUNT(t) AS transactionCount
            FROM Transaction t
            GROUP BY t.status
            """)
    List<StatusCountProjection> countByStatus();

    @Query(value = """
            SELECT date_trunc('day', created_at)::date AS day,
                   COALESCE(SUM(amount), 0) AS total_amount,
                   COUNT(*) AS transaction_count
            FROM transactions
            WHERE created_at >= :since
              AND status = 'COMPLETED'
            GROUP BY day
            ORDER BY day ASC
            """, nativeQuery = true)
    List<Object[]> dailyVolumeRaw(@Param("since") Instant since);

    interface StatusCountProjection {
        TransactionStatus getStatus();

        Long getTransactionCount();
    }
}