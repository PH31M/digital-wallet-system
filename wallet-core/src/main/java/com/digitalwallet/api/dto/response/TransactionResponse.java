package com.digitalwallet.api.dto.response;

import com.digitalwallet.domain.entity.Transaction;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class TransactionResponse {

    private UUID id;

    @JsonProperty("reference_number")
    private String referenceNumber;

    @JsonProperty("sender_wallet_id")
    private UUID senderWalletId;

    @JsonProperty("receiver_wallet_id")
    private UUID receiverWalletId;

    @JsonProperty("transaction_type")
    private String transactionType;

    private BigDecimal amount;
    private String status;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("completed_at")
    private Instant completedAt;

    @JsonProperty("failed_at")
    private Instant failedAt;

    public TransactionResponse() {
    }

    public TransactionResponse(UUID id, String referenceNumber, UUID senderWalletId, UUID receiverWalletId,
            String transactionType, BigDecimal amount, String status, Instant createdAt,
            Instant completedAt, Instant failedAt) {
        this.id = id;
        this.referenceNumber = referenceNumber;
        this.senderWalletId = senderWalletId;
        this.receiverWalletId = receiverWalletId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
        this.failedAt = failedAt;
    }

    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getReferenceNumber(),
                transaction.getSenderWallet() == null ? null : transaction.getSenderWallet().getId(),
                transaction.getReceiverWallet() == null ? null : transaction.getReceiverWallet().getId(),
                transaction.getTransactionType().name(),
                transaction.getAmount(),
                transaction.getStatus().name(),
                transaction.getCreatedAt(),
                transaction.getCompletedAt(),
                transaction.getFailedAt());
    }

    public UUID getId() {
        return id;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public UUID getSenderWalletId() {
        return senderWalletId;
    }

    public UUID getReceiverWalletId() {
        return receiverWalletId;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getFailedAt() {
        return failedAt;
    }
}