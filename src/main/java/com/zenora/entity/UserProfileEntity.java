package com.zenora.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "user_profiles")
public class UserProfileEntity extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private Integer age = 25;
    private Double monthlyIncome = 0.0;
    private Double monthlyExpense = 0.0;
    private Double monthlyCapacityOverride = 0.0;
    private Integer emergencyMonths = 6;
    private String householdStatus = "Single";

    /** Username pemilik data — auto-diisi oleh API controller. */
    @Column(name = "owner_username")
    private String ownerUsername;
    private Double inflationPct = 3.5;

    // ── Constructors ───────────────────────────────────────────────────────

    public UserProfileEntity() {}

    public UserProfileEntity(String name, Integer age,
                              Double monthlyIncome, Double monthlyExpense) {
        this.name           = name;
        this.age            = age;
        this.monthlyIncome  = monthlyIncome;
        this.monthlyExpense = monthlyExpense;
    }

    // ── BaseEntity abstract ────────────────────────────────────────────────
    @Override
    public String getDisplayName() {
        return "UserProfile[" + name + ", age=" + age + "]";
    }

    // ── Business logic (Encapsulation) ────────────────────────────────────

    /** Kapasitas menabung efektif per bulan. */
    public double effectiveCapacity() {
        if (monthlyCapacityOverride != null && monthlyCapacityOverride > 0)
            return monthlyCapacityOverride;
        return Math.max(0, (monthlyIncome != null ? monthlyIncome : 0)
                         - (monthlyExpense != null ? monthlyExpense : 0));
    }

    // ── Getters & Setters ─────────────────────────────────────────────────

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public Double getMonthlyIncome() { return monthlyIncome; }
    public void setMonthlyIncome(Double monthlyIncome) { this.monthlyIncome = monthlyIncome; }

    public Double getMonthlyExpense() { return monthlyExpense; }
    public void setMonthlyExpense(Double monthlyExpense) { this.monthlyExpense = monthlyExpense; }

    public Double getMonthlyCapacityOverride() { return monthlyCapacityOverride; }
    public void setMonthlyCapacityOverride(Double v) { this.monthlyCapacityOverride = v; }

    public Integer getEmergencyMonths() { return emergencyMonths; }
    public void setEmergencyMonths(Integer v) { this.emergencyMonths = v; }

    public String getHouseholdStatus() { return householdStatus; }
    public void setHouseholdStatus(String v) { this.householdStatus = v; }

    public Double getInflationPct() { return inflationPct; }
    public void setInflationPct(Double v) { this.inflationPct = v; }
    public String getOwnerUsername() { return ownerUsername; }
    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; }
}
