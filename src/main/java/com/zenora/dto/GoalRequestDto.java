package com.zenora.dto;

import jakarta.validation.constraints.*;

/**
 * ✅ PR-3: DTO untuk request pembuatan/update Goal.
 *
 * ✅ KETENTUAN — Validation:
 *   Anotasi dari jakarta.validation memastikan input valid
 *   SEBELUM sampai ke Service/Repository.
 *
 * ✅ OOP PILAR — ENCAPSULATION:
 *   Field private, diakses via getter/setter.
 *   Data transfer object tidak mengekspos entity langsung.
 */
public class GoalRequestDto {

    @NotBlank(message = "Nama goal tidak boleh kosong")
    @Size(min = 2, max = 100, message = "Nama harus antara 2-100 karakter")
    private String name;

    @NotNull(message = "Target amount wajib diisi")
    @Positive(message = "Target amount harus lebih dari 0")
    private Double targetAmount;

    @NotNull(message = "Jumlah bulan wajib diisi")
    @Min(value = 1, message = "Minimal 1 bulan")
    @Max(value = 600, message = "Maksimal 600 bulan (50 tahun)")
    private Integer months;

    @Min(value = 0, message = "Suku bunga tidak boleh negatif")
    @Max(value = 100, message = "Suku bunga maksimal 100%")
    private Double interestRate = 0.0;

    @Min(value = 1, message = "Priority minimal 1")
    @Max(value = 5, message = "Priority maksimal 5")
    private Integer priority = 3;

    private String category = "UMUM";
    private String storageType = "Bank";
    private String storageLocation = "";

    // ── Getters & Setters (Encapsulation) ─────────────────────────────────

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getTargetAmount() { return targetAmount; }
    public void setTargetAmount(Double targetAmount) { this.targetAmount = targetAmount; }

    public Integer getMonths() { return months; }
    public void setMonths(Integer months) { this.months = months; }

    public Double getInterestRate() { return interestRate; }
    public void setInterestRate(Double interestRate) { this.interestRate = interestRate; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getStorageType() { return storageType; }
    public void setStorageType(String v) { this.storageType = v; }

    public String getStorageLocation() { return storageLocation; }
    public void setStorageLocation(String v) { this.storageLocation = v; }
}
