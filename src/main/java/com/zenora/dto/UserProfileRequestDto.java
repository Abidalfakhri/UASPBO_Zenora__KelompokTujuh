package com.zenora.dto;

import jakarta.validation.constraints.*;


public class UserProfileRequestDto {

    @NotBlank(message = "Nama tidak boleh kosong")
    @Size(min = 2, max = 100, message = "Nama harus 2-100 karakter")
    private String name;

    @Min(value = 17, message = "Usia minimal 17 tahun")
    @Max(value = 100, message = "Usia maksimal 100 tahun")
    private Integer age;

    @Min(value = 0, message = "Pendapatan tidak boleh negatif")
    private Double monthlyIncome;

    @Min(value = 0, message = "Pengeluaran tidak boleh negatif")
    private Double monthlyExpense;

    private Integer emergencyMonths = 6;
    private String householdStatus  = "Single";

    @DecimalMin(value = "0.0", message = "Inflasi tidak boleh negatif")
    @DecimalMax(value = "50.0", message = "Inflasi maksimal 50%")
    private Double inflationPct = 3.5;

    // ── Getters & Setters ─────────────────────────────────────────────────

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public Double getMonthlyIncome() { return monthlyIncome; }
    public void setMonthlyIncome(Double v) { this.monthlyIncome = v; }

    public Double getMonthlyExpense() { return monthlyExpense; }
    public void setMonthlyExpense(Double v) { this.monthlyExpense = v; }

    public Integer getEmergencyMonths() { return emergencyMonths; }
    public void setEmergencyMonths(Integer v) { this.emergencyMonths = v; }

    public String getHouseholdStatus() { return householdStatus; }
    public void setHouseholdStatus(String v) { this.householdStatus = v; }

    public Double getInflationPct() { return inflationPct; }
    public void setInflationPct(Double v) { this.inflationPct = v; }
}
