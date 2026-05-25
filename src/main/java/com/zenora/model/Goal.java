package com.zenora.model;

import javafx.beans.property.*;

import java.time.LocalDate;
import java.util.UUID;

public class Goal {
    private String id = UUID.randomUUID().toString();
    private LocalDate createdAt = LocalDate.now();
    private LocalDate targetDate;
    private Category category = Category.UMUM;
    private String storageType = "Bank";
    private String storageLocation = "";

    private final transient StringProperty name = new SimpleStringProperty("");
    private final transient DoubleProperty targetAmount = new SimpleDoubleProperty(0);
    private final transient IntegerProperty months = new SimpleIntegerProperty(12);
    private final transient DoubleProperty interestRate = new SimpleDoubleProperty(0);
    private final transient IntegerProperty priority = new SimpleIntegerProperty(3);
    private final transient DoubleProperty monthlySaving = new SimpleDoubleProperty(0);
    private final transient DoubleProperty currentSaving = new SimpleDoubleProperty(0);

    public Goal() {}

    public Goal(String name, double targetAmount, int months, double interestRate, int priority) {
        setName(name);
        setTargetAmount(targetAmount);
        setMonths(months);
        setInterestRate(interestRate);
        setPriority(priority);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public LocalDate getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDate v) { this.createdAt = v; }

    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate v) { this.targetDate = v; }

    public Category getCategory() { return category == null ? Category.UMUM : category; }
    public void setCategory(Category v) { this.category = v; }

    public String getStorageType() { return storageType == null ? "Bank" : storageType; }
    public void setStorageType(String v) { this.storageType = v; }

    public String getStorageLocation() { return storageLocation == null ? "" : storageLocation; }
    public void setStorageLocation(String v) { this.storageLocation = v; }

    public String getName() { return name.get(); }
    public void setName(String v) { name.set(v == null ? "" : v); }
    public StringProperty nameProperty() { return name; }

    public double getTargetAmount() { return targetAmount.get(); }
    public void setTargetAmount(double v) { targetAmount.set(v); }
    public DoubleProperty targetAmountProperty() { return targetAmount; }

    public int getMonths() { return months.get(); }
    public void setMonths(int v) { months.set(v); }
    public IntegerProperty monthsProperty() { return months; }

    public double getInterestRate() { return interestRate.get(); }
    public void setInterestRate(double v) { interestRate.set(v); }
    public DoubleProperty interestRateProperty() { return interestRate; }

    public int getPriority() { return priority.get(); }
    public void setPriority(int v) { priority.set(v); }
    public IntegerProperty priorityProperty() { return priority; }

    public double getMonthlySaving() { return monthlySaving.get(); }
    public void setMonthlySaving(double v) { monthlySaving.set(v); }
    public DoubleProperty monthlySavingProperty() { return monthlySaving; }

    public double getCurrentSaving() { return currentSaving.get(); }
    public void setCurrentSaving(double v) { currentSaving.set(v); }
    public DoubleProperty currentSavingProperty() { return currentSaving; }

    public double getProgressPercent() {
        if (targetAmount.get() == 0) return 0;
        return Math.min(100, (currentSaving.get() / targetAmount.get()) * 100);
    }

    @Override public String toString() { return getName(); }
}
