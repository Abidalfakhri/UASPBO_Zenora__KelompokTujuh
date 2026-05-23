package com.zenora.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * ✅ PR-1: JPA Entity untuk tabel GOALS di H2 Database.
 *
 * ✅ OOP PILAR — INHERITANCE:
 *   Mewarisi id, createdAt, updatedAt dari BaseEntity.
 *   Tidak perlu menulis ulang field audit.
 *
 * ✅ OOP PILAR — ENCAPSULATION:
 *   Semua field private, diakses via getter/setter.
 *
 * Tabel ini dikelola otomatis oleh JPA/Hibernate (ddl-auto=create-drop).
 */
@Entity
@Table(name = "goals")
public class GoalEntity extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Double targetAmount;

    @Column(nullable = false)
    private Integer months;

    private Double interestRate = 0.0;
    private Integer priority = 3;
    private Double monthlySaving = 0.0;
    private Double currentSaving = 0.0;
    private LocalDate targetDate;

    @Column(nullable = false)
    private String category = "UMUM";

    // ── Constructors ───────────────────────────────────────────────────────

    public GoalEntity() {}

    public GoalEntity(String name, Double targetAmount, Integer months,
                      Double interestRate, Integer priority) {
        this.name          = name;
        this.targetAmount  = targetAmount;
        this.months        = months;
        this.interestRate  = interestRate;
        this.priority      = priority;
    }

    // ── BaseEntity abstract ────────────────────────────────────────────────
    @Override
    public String getDisplayName() {
        return "Goal[" + name + ", target=" + targetAmount + "]";
    }

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

    public Double getMonthlySaving() { return monthlySaving; }
    public void setMonthlySaving(Double monthlySaving) { this.monthlySaving = monthlySaving; }

    public Double getCurrentSaving() { return currentSaving; }
    public void setCurrentSaving(Double currentSaving) { this.currentSaving = currentSaving; }

    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
