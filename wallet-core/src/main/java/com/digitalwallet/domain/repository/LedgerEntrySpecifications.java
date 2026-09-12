package com.digitalwallet.domain.repository;

import com.digitalwallet.domain.entity.LedgerEntry;
import com.digitalwallet.domain.enums.TransactionStatus;
import com.digitalwallet.domain.enums.TransactionType;
import com.digitalwallet.exception.BusinessException;
import com.digitalwallet.exception.ErrorCode;
import jakarta.persistence.criteria.Expression;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

public final class LedgerEntrySpecifications {

    private LedgerEntrySpecifications() {
    }

    public static Specification<LedgerEntry> walletIdIn(Collection<UUID> walletIds) {
        return (root, query, criteriaBuilder) -> root.get("wallet").get("id").in(walletIds);
    }

    public static Specification<LedgerEntry> typeEquals(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        TransactionType transactionType = parseType(type);
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(
                root.get("transaction").get("transactionType"), transactionType);
    }

    public static Specification<LedgerEntry> statusEquals(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        TransactionStatus transactionStatus = parseStatus(status);
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(
                root.get("transaction").get("status"), transactionStatus);
    }

    public static Specification<LedgerEntry> createdBetween(Instant from, Instant to) {
        return (root, query, criteriaBuilder) -> {
            if (from == null && to == null) {
                return null;
            }
            if (from != null && to != null) {
                return criteriaBuilder.between(root.get("createdAt"), from, to);
            }
            return from != null
                    ? criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), from)
                    : criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), to);
        };
    }

    public static Specification<LedgerEntry> amountBetween(BigDecimal min, BigDecimal max) {
        return (root, query, criteriaBuilder) -> {
            if (min == null && max == null) {
                return null;
            }

            Expression<BigDecimal> amount = criteriaBuilder.<BigDecimal>selectCase()
                    .when(criteriaBuilder.greaterThan(root.get("debitAmount"), BigDecimal.ZERO),
                            root.get("debitAmount"))
                    .otherwise(root.get("creditAmount"));
            if (min != null && max != null) {
                return criteriaBuilder.between(amount, min, max);
            }
            return min != null
                    ? criteriaBuilder.greaterThanOrEqualTo(amount, min)
                    : criteriaBuilder.lessThanOrEqualTo(amount, max);
        };
    }

    private static TransactionType parseType(String type) {
        try {
            return TransactionType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Unknown transaction type: " + type);
        }
    }

    private static TransactionStatus parseStatus(String status) {
        try {
            return TransactionStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Unknown transaction status: " + status);
        }
    }
}