package com.zenora.model;

/** Personal financial profile — single source of truth for income, expenses, capacity. */
public class UserProfile {
    private String name = "";
    private int age = 0;
    private double monthlyIncome = 0;
    private double monthlyExpense = 0;
    /** Override capacity; if 0 we derive it from income - expense. */
    private double monthlyCapacityOverride = 0;
    /** Recommended emergency-fund coverage in months (3/6/12). */
    private int emergencyMonths = 6;
    /** Marital / dependent status — used to pre-set emergency months. */
    private String householdStatus = "Single";
    /** Estimated annual inflation used by all modules (default Indonesia ~3%). */
    private double inflationPct = 3.5;

    public String getName() { return name == null ? "" : name; }
    public void setName(String name) { this.name = name; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public double getMonthlyIncome() { return monthlyIncome; }
    public void setMonthlyIncome(double v) { this.monthlyIncome = v; }
    public double getMonthlyExpense() { return monthlyExpense; }
    public void setMonthlyExpense(double v) { this.monthlyExpense = v; }
    public double getMonthlyCapacityOverride() { return monthlyCapacityOverride; }
    public void setMonthlyCapacityOverride(double v) { this.monthlyCapacityOverride = v; }
    public int getEmergencyMonths() { return emergencyMonths; }
    public void setEmergencyMonths(int v) { this.emergencyMonths = v; }
    public String getHouseholdStatus() { return householdStatus == null ? "Single" : householdStatus; }
    public void setHouseholdStatus(String v) { this.householdStatus = v; }
    public double getInflationPct() { return inflationPct; }
    public void setInflationPct(double v) { this.inflationPct = v; }

    /** Derived: monthly cash that can go to goals. Override wins if > 0. */
    public double effectiveCapacity() {
        if (monthlyCapacityOverride > 0) return monthlyCapacityOverride;
        return Math.max(0, monthlyIncome - monthlyExpense);
    }

    /** Recommended emergency fund based on expenses & coverage months. */
    public double recommendedEmergencyFund() {
        return monthlyExpense * Math.max(1, emergencyMonths);
    }

    /** Savings rate 0..1. */
    public double savingsRate() {
        if (monthlyIncome <= 0) return 0;
        return Math.max(0, Math.min(1, (monthlyIncome - monthlyExpense) / monthlyIncome));
    }
}
