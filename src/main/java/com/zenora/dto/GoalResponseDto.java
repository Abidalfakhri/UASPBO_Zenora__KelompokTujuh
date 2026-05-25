package com.zenora.dto;

import com.zenora.entity.GoalEntity;

/**
 * ✅ PR-3: DTO untuk response Goal ke client (JavaFX / Postman).
 *
 * ✅ OOP PILAR — ENCAPSULATION:
 *   Tidak mengekspos GoalEntity langsung, hanya data yang diperlukan.
 *   Method fromEntity() sebagai factory/converter.
 */
public class GoalResponseDto {

    private String id;
    private String name;
    private Double targetAmount;
    private Double currentSaving;
    private Integer months;
    private Double interestRate;
    private Integer priority;
    private String category;
    private Double progressPercent;
    private Double monthlySaving;
    private String createdAt;
    private String storageType;
    private String storageLocation;

    /** Factory method — konversi dari Entity ke DTO. */
    public static GoalResponseDto fromEntity(GoalEntity e) {
        GoalResponseDto dto = new GoalResponseDto();
        dto.id              = e.getId();
        dto.name            = e.getName();
        dto.targetAmount    = e.getTargetAmount();
        dto.currentSaving   = e.getCurrentSaving() != null ? e.getCurrentSaving() : 0.0;
        dto.months          = e.getMonths();
        dto.interestRate    = e.getInterestRate();
        dto.priority        = e.getPriority();
        dto.category        = e.getCategory();
        dto.monthlySaving   = e.getMonthlySaving();
        dto.createdAt       = e.getCreatedAt() != null ? e.getCreatedAt().toString() : null;
        dto.storageType     = e.getStorageType();
        dto.storageLocation = e.getStorageLocation();
        // Hitung progress percent
        if (e.getTargetAmount() != null && e.getTargetAmount() > 0 && e.getCurrentSaving() != null) {
            dto.progressPercent = Math.min(100.0, (e.getCurrentSaving() / e.getTargetAmount()) * 100);
        } else {
            dto.progressPercent = 0.0;
        }
        return dto;
    }

    // ── Getters ────────────────────────────────────────────────────────────

    public String getId() { return id; }
    public String getName() { return name; }
    public Double getTargetAmount() { return targetAmount; }
    public Double getCurrentSaving() { return currentSaving; }
    public Integer getMonths() { return months; }
    public Double getInterestRate() { return interestRate; }
    public Integer getPriority() { return priority; }
    public String getCategory() { return category; }
    public Double getProgressPercent() { return progressPercent; }
    public Double getMonthlySaving() { return monthlySaving; }
    public String getCreatedAt() { return createdAt; }
    public String getStorageType() { return storageType; }
    public String getStorageLocation() { return storageLocation; }
}
