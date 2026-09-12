package com.digitalwallet.api.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Wallet response payload.
 */
public class WalletResponse {

    private UUID id;
    private String currency;
    private String status;
    private BigDecimal balance;

    public WalletResponse() {
    }

    public WalletResponse(UUID id, String currency, String status, BigDecimal balance) {
        this.id = id;
        this.currency = currency;
        this.status = status;
        this.balance = balance;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
