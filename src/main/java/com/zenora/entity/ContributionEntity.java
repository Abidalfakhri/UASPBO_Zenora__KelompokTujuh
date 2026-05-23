package com.zenora.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * ✅ PR-1: JPA Entity untuk tabel CONTRIBUTIONS di H2 Database.
 *
 * ✅ OOP PILAR — INHERITANCE: extends BaseEntity (id, timestamps otomatis).
 * ✅ OOP PILAR — ENCAPSULATION: field private, akses via getter/setter.
 */
@Entity
@Table(name = "contributions")
public class ContributionEntity extends BaseEntity {

    @Column(nullable = false)
    private String goalId;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private LocalDate date;

    private String note;

    // ── Constructors ───────────────────────────────────────────────────────

    public ContributionEntity() {}

    public ContributionEntity(String goalId, Double amount, LocalDate date, String note) {
        this.goalId = goalId;
        this.amount = amount;
        this.date   = date;
        this.note   = note;
    }

    // ── BaseEntity abstract ────────────────────────────────────────────────
    @Override
    public String getDisplayName() {
        return "Contribution[goalId=" + goalId + ", amount=" + amount + "]";
    }

    // ── Getters & Setters (Encapsulation) ─────────────────────────────────

    public String getGoalId() { return goalId; }
    public void setGoalId(String goalId) { this.goalId = goalId; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
