package com.digitalwallet.service;

import com.digitalwallet.api.dto.request.TransactionHistoryFilter;
import com.digitalwallet.api.dto.response.TransactionHistoryItemResponse;
import com.digitalwallet.domain.entity.LedgerEntry;
import com.digitalwallet.domain.repository.LedgerEntryRepository;
import com.digitalwallet.domain.repository.LedgerEntrySpecifications;
import com.digitalwallet.domain.repository.WalletRepository;
import com.digitalwallet.exception.BusinessException;
import com.digitalwallet.exception.ErrorCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class TransactionService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt");

    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public TransactionService(WalletRepository walletRepository, LedgerEntryRepository ledgerEntryRepository) {
        this.walletRepository = walletRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional(readOnly = true)
    public Page<TransactionHistoryItemResponse> getHistory(
            UUID currentUserId,
            TransactionHistoryFilter filter,
            Pageable pageable) {
        TransactionHistoryFilter safeFilter = filter == null ? emptyFilter() : filter;
        validateFilter(safeFilter);

        List<UUID> ownedWalletIds = walletRepository.findWalletIdsByUserId(currentUserId);
        if (ownedWalletIds.isEmpty()) {
            return Page.empty(clampPageable(pageable));
        }

        Specification<LedgerEntry> specification = LedgerEntrySpecifications.walletIdIn(ownedWalletIds)
                .and(LedgerEntrySpecifications.typeEquals(safeFilter.type()))
                .and(LedgerEntrySpecifications.statusEquals(safeFilter.status()))
                .and(LedgerEntrySpecifications.createdBetween(safeFilter.dateFrom(), safeFilter.dateTo()))
                .and(LedgerEntrySpecifications.amountBetween(safeFilter.minAmount(), safeFilter.maxAmount()));

        return ledgerEntryRepository.findAll(specification, clampPageable(pageable))
                .map(this::toResponse);
    }

    private Pageable clampPageable(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, DEFAULT_PAGE_SIZE, DEFAULT_SORT);
        }

        int size = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        return PageRequest.of(pageable.getPageNumber(), size, resolveSort(pageable.getSort()));
    }

    private Sort resolveSort(Sort requested) {
        if (requested == null || requested.isUnsorted()) {
            return DEFAULT_SORT;
        }

        List<Sort.Order> safeOrders = requested.stream()
                .filter(order -> ALLOWED_SORT_FIELDS.contains(order.getProperty()))
                .toList();
        return safeOrders.isEmpty() ? DEFAULT_SORT : Sort.by(safeOrders);
    }

    private TransactionHistoryItemResponse toResponse(LedgerEntry entry) {
        BigDecimal debitAmount = nonNullAmount(entry.getDebitAmount());
        BigDecimal creditAmount = nonNullAmount(entry.getCreditAmount());
        boolean debit = debitAmount.compareTo(BigDecimal.ZERO) > 0;
        BigDecimal amount = debit ? debitAmount : creditAmount;
        String direction = debit ? "DEBIT" : "CREDIT";

        return new TransactionHistoryItemResponse(
                entry.getId(),
                entry.getTransaction().getId(),
                entry.getWallet().getId(),
                entry.getTransaction().getTransactionType().name(),
                direction,
                amount,
                null,
                entry.getTransaction().getStatus().name(),
                entry.getTransaction().getReferenceNumber(),
                entry.getCreatedAt());
    }

    private void validateFilter(TransactionHistoryFilter filter) {
        LedgerEntrySpecifications.typeEquals(filter.type());
        LedgerEntrySpecifications.statusEquals(filter.status());

        if (filter.minAmount() != null && filter.minAmount().signum() < 0
                || filter.maxAmount() != null && filter.maxAmount().signum() < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Amount filters must not be negative");
        }
        if (filter.minAmount() != null && filter.maxAmount() != null
                && filter.minAmount().compareTo(filter.maxAmount()) > 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "minAmount must not exceed maxAmount");
        }
        if (filter.dateFrom() != null && filter.dateTo() != null
                && filter.dateFrom().isAfter(filter.dateTo())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "dateFrom must not be after dateTo");
        }
    }

    private TransactionHistoryFilter emptyFilter() {
        return new TransactionHistoryFilter(null, null, null, null, null, null);
    }

    private BigDecimal nonNullAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}