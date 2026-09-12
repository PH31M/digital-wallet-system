package com.digitalwallet.api.dto.response;

import com.digitalwallet.domain.entity.LedgerEntry;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class LedgerEntryResponse {

    private UUID id;

    @JsonProperty("transaction_id")
    private UUID transactionId;

    @JsonProperty("wallet_id")
    private UUID walletId;

    @JsonProperty("account_type")
    private String accountType;

    @JsonProperty("debit_amount")
    private BigDecimal debitAmount;

    @JsonProperty("credit_amount")
    private BigDecimal creditAmount;

    @JsonProperty("created_at")
    private Instant createdAt;

    public LedgerEntryResponse() {
    }

    public LedgerEntryResponse(UUID id, UUID transactionId, UUID walletId, String accountType,
            BigDecimal debitAmount, BigDecimal creditAmount, Instant createdAt) {
        this.id = id;
        this.transactionId = transactionId;
        this.walletId = walletId;
        this.accountType = accountType;
        this.debitAmount = debitAmount;
        this.creditAmount = creditAmount;
        this.createdAt = createdAt;
    }

    public static LedgerEntryResponse from(LedgerEntry entry) {
        return new LedgerEntryResponse(
                entry.getId(),
                entry.getTransaction().getId(),
                entry.getWallet() == null ? null : entry.getWallet().getId(),
                entry.getAccountType().name(),
                entry.getDebitAmount(),
                entry.getCreditAmount(),
                entry.getCreatedAt());
    }

    public UUID getId() {
        return id;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public UUID getWalletId() {
        return walletId;
    }

    public String getAccountType() {
        return accountType;
    }

    public BigDecimal getDebitAmount() {
        return debitAmount;
    }

    public BigDecimal getCreditAmount() {
        return creditAmount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}