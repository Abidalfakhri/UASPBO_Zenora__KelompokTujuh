package com.zenora.model;

import java.time.LocalDate;
import java.util.UUID;

/** Single deposit / contribution to a goal. */
public class Contribution {
    private String id = UUID.randomUUID().toString();
    private String goalId;
    private LocalDate date = LocalDate.now();
    private double amount;
    private String note = "";

    public Contribution() {}

    public Contribution(String goalId, LocalDate date, double amount, String note) {
        this.goalId = goalId;
        this.date = date;
        this.amount = amount;
        this.note = note == null ? "" : note;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getGoalId() { return goalId; }
    public void setGoalId(String goalId) { this.goalId = goalId; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note == null ? "" : note; }
}
