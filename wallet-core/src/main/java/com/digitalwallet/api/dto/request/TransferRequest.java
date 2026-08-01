package com.digitalwallet.api.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public class TransferRequest {

    @JsonProperty("receiver_wallet_id")
    @NotNull(message = "Receiver wallet id is required")
    private UUID receiverWalletId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @JsonProperty("idempotency_key")
    private String idempotencyKey;

    public TransferRequest() {
    }

    public TransferRequest(UUID receiverWalletId, BigDecimal amount, String idempotencyKey) {
        this.receiverWalletId = receiverWalletId;
        this.amount = amount;
        this.idempotencyKey = idempotencyKey;
    }

    public UUID getReceiverWalletId() {
        return receiverWalletId;
    }

    public void setReceiverWalletId(UUID receiverWalletId) {
        this.receiverWalletId = receiverWalletId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}