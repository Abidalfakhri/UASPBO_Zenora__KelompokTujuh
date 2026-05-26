package com.zenora.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;


public class ContributionRequestDto {

    @NotBlank(message = "goalId tidak boleh kosong")
    private String goalId;

    @NotNull(message = "Amount wajib diisi")
    @Positive(message = "Amount harus lebih dari 0")
    private Double amount;

    @NotNull(message = "Tanggal wajib diisi")
    private LocalDate date;

    @Size(max = 255, message = "Catatan maksimal 255 karakter")
    private String note;

    // ── Getters & Setters ─────────────────────────────────────────────────

    public String getGoalId() { return goalId; }
    public void setGoalId(String goalId) { this.goalId = goalId; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
