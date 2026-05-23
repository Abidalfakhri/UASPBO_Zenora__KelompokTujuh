package com.zenora.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * ✅ PR-1: Abstract base class untuk semua JPA Entity di Zenora.
 *
 * ✅ OOP PILAR — ABSTRACTION:
 *   Class ini tidak bisa diinstansiasi langsung (abstract).
 *   Menyembunyikan detail teknis JPA (id, audit timestamps) dari subclass.
 *
 * ✅ OOP PILAR — INHERITANCE:
 *   GoalEntity, ContributionEntity, UserProfileEntity, AppUser
 *   semuanya mewarisi field id, createdAt, updatedAt dari sini.
 *
 * ✅ OOP PILAR — ENCAPSULATION:
 *   Field bersifat private/protected, hanya bisa diakses via getter/setter.
 */
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private String id;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ── Getters & Setters (Encapsulation) ────────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }

    /**
     * ✅ OOP — ABSTRACTION:
     * Setiap entity wajib mengimplementasi method ini
     * untuk keperluan logging dan audit.
     */
    public abstract String getDisplayName();
}
