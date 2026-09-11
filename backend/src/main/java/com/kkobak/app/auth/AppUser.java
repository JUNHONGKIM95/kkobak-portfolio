package com.kkobak.app.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "app_users")
public class AppUser {
    @Id private String id;
    @Column(nullable = false, unique = true, length = 40) private String username;
    @Column(name = "password_hash", nullable = false, length = 255) private String passwordHash;
    @Column(name = "display_name", nullable = false, length = 80) private String displayName;
    @Column(nullable = false, length = 255) private String email;
    @Column(nullable = false, length = 20) private String role;
    @Column(nullable = false, length = 20) private String status;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    @Column(name = "approved_at") private LocalDateTime approvedAt;
    @Column(name = "last_login_at") private LocalDateTime lastLoginAt;

    protected AppUser() { }

    public AppUser(String username, String passwordHash, String displayName, String email, String role, String status) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.email = email;
        this.role = role;
        this.status = status;
    }

    public void approve() { status = "APPROVED"; approvedAt = LocalDateTime.now(); }
    public void reject() { status = "REJECTED"; approvedAt = null; }
    public void markLogin() { lastLoginAt = LocalDateTime.now(); }
    public void resetPassword(String passwordHash) { this.passwordHash = passwordHash; }

    @PrePersist
    void create() {
        if (id == null) id = UUID.randomUUID().toString();
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate void updateTimestamp() { updatedAt = LocalDateTime.now(); }

    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
}
