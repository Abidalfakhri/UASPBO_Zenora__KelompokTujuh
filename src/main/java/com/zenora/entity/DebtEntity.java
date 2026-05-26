package com.zenora.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "debts")
public class DebtEntity extends BaseEntity {

    @Column(nullable = false)
    private String name;

    /** Saldo hutang saat ini (berkurang saat ada setoran). */
    @Column(nullable = false)
    private Double balance;

    /** Saldo awal saat hutang pertama kali dicatat. */
    @Column(nullable = false)
    private Double originalBalance;

    /** Annual Percentage Rate dalam persen (misal: 12.5 = 12.5%). */
    @Column(nullable = false)
    private Double aprPercent = 0.0;

    /** Cicilan minimum per bulan. */
    @Column(nullable = false)
    private Double minimumPayment = 0.0;

    /** Akumulasi total yang sudah dibayarkan. */
    @Column(nullable = false)
    private Double totalPaid = 0.0;

    /** Username pemilik data. */
    @Column(name = "owner_username", nullable = false)
    private String ownerUsername;

    public DebtEntity() {}

    @Override
    public String getDisplayName() {
        return "Debt[" + name + ", balance=" + balance + "]";
    }

    // ── Getters & Setters ─────────────────────────────────────────────────

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getBalance() { return balance; }
    public void setBalance(Double balance) { this.balance = balance; }

    public Double getOriginalBalance() { return originalBalance; }
    public void setOriginalBalance(Double originalBalance) { this.originalBalance = originalBalance; }

    public Double getAprPercent() { return aprPercent; }
    public void setAprPercent(Double aprPercent) { this.aprPercent = aprPercent; }

    public Double getMinimumPayment() { return minimumPayment; }
    public void setMinimumPayment(Double minimumPayment) { this.minimumPayment = minimumPayment; }

    public Double getTotalPaid() { return totalPaid; }
    public void setTotalPaid(Double totalPaid) { this.totalPaid = totalPaid; }

    public String getOwnerUsername() { return ownerUsername; }
    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; }
}
