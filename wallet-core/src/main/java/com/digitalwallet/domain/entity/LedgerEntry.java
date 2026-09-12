package com.digitalwallet.domain.entity;

import java.math.BigDecimal;

import com.digitalwallet.domain.enums.LedgerAccountType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Ledger entry entity.
 */
@Entity
@Table(name = "ledger_entries")
public class LedgerEntry extends BaseEntity {

    protected LedgerEntry() {
    }

    public LedgerEntry(Transaction transaction, Wallet wallet, LedgerAccountType accountType,
            BigDecimal debitAmount, BigDecimal creditAmount) {
        this.transaction = transaction;
        this.wallet = wallet;
        this.accountType = accountType;
        this.debitAmount = debitAmount;
        this.creditAmount = creditAmount;
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private LedgerAccountType accountType;

    @Column(name = "debit_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal debitAmount = BigDecimal.ZERO;

    @Column(name = "credit_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal creditAmount = BigDecimal.ZERO;

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public void setWallet(Wallet wallet) {
        this.wallet = wallet;
    }

    public LedgerAccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(LedgerAccountType accountType) {
        this.accountType = accountType;
    }

    public BigDecimal getDebitAmount() {
        return debitAmount;
    }

    public void setDebitAmount(BigDecimal debitAmount) {
        this.debitAmount = debitAmount;
    }

    public BigDecimal getCreditAmount() {
        return creditAmount;
    }

    public void setCreditAmount(BigDecimal creditAmount) {
        this.creditAmount = creditAmount;
    }

}
