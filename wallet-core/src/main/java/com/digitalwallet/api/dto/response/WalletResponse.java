package com.digitalwallet.api.dto.response;

/**
 * Wallet response payload.
 */
public class WalletResponse {

    private Long id;
    private String walletNumber;
    private String status;
    private Long balance;

    public WalletResponse() {
    }

    public WalletResponse(Long id, String walletNumber, String status, Long balance) {
        this.id = id;
        this.walletNumber = walletNumber;
        this.status = status;
        this.balance = balance;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getWalletNumber() {
        return walletNumber;
    }

    public void setWalletNumber(String walletNumber) {
        this.walletNumber = walletNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getBalance() {
        return balance;
    }

    public void setBalance(Long balance) {
        this.balance = balance;
    }
}
