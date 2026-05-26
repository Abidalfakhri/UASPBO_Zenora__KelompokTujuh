package com.zenora.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public class DebtRequestDto {

    @NotBlank(message = "Nama hutang tidak boleh kosong")
    private String name;

    @NotNull(message = "Saldo hutang wajib diisi")
    @PositiveOrZero(message = "Saldo hutang tidak boleh negatif")
    private Double balance;

    @NotNull(message = "Saldo awal wajib diisi")
    @PositiveOrZero(message = "Saldo awal tidak boleh negatif")
    private Double originalBalance;

    @PositiveOrZero(message = "APR tidak boleh negatif")
    private Double aprPercent = 0.0;

    @PositiveOrZero(message = "Minimum payment tidak boleh negatif")
    private Double minimumPayment = 0.0;

    @PositiveOrZero(message = "Total paid tidak boleh negatif")
    private Double totalPaid = 0.0;

    // ── Getters & Setters ──────────────────────────────────────────────────

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
}
