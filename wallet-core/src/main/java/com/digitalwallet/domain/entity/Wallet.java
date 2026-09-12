package com.digitalwallet.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;

import com.digitalwallet.domain.enums.WalletStatus;

/**
 * Wallet entity.
 */
@Entity
@Table(name = "wallets", uniqueConstraints = { @UniqueConstraint(columnNames = { "user_id", "currency" }) })
public class Wallet extends BaseAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_wallet_user", foreignKeyDefinition = "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT"))
    private User user;

    @Column(nullable = false, length = 3)
    private String currency = "VND";

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WalletStatus status = WalletStatus.PENDING_VERIFICATION;

    public User getUser() {
        return user;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public WalletStatus getStatus() {
        return status;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public void setStatus(WalletStatus status) {
        this.status = status;
    }

    public void activate() {
        this.status = WalletStatus.ACTIVE;
    }

    public void credit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    public void debit(BigDecimal amount) {
        this.balance = this.balance.subtract(amount);
    }
}
