package com.zenora.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


@Entity
@Table(name = "app_users")
public class AppUser extends BaseEntity {

    @NotBlank
    @Size(min = 3, max = 50)
    @Column(unique = true, nullable = false)
    private String username;

    @NotBlank
    @Column(nullable = false)
    private String password; // Disimpan dalam format BCrypt hash

    @Column(nullable = false)
    private String role = "ROLE_USER"; // ROLE_USER atau ROLE_ADMIN

    @Column(nullable = false)
    private boolean enabled = true;

    // ── Constructors ───────────────────────────────────────────────────────

    public AppUser() {}

    public AppUser(String username, String encodedPassword, String role) {
        this.username = username;
        this.password = encodedPassword;
        this.role     = role;
    }

    // ── BaseEntity abstract ────────────────────────────────────────────────
    @Override
    public String getDisplayName() {
        return "AppUser[" + username + ", role=" + role + "]";
    }

    // ── Getters & Setters (Encapsulation) ─────────────────────────────────

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
