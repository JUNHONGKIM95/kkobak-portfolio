package com.kkobak.app.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_sessions")
public class UserSession {
    @Id private String id;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false) private AppUser user;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(name = "expires_at", nullable = false) private LocalDateTime expiresAt;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;

    protected UserSession() { }
    public UserSession(AppUser user, String tokenHash, LocalDateTime expiresAt) { this.user = user; this.tokenHash = tokenHash; this.expiresAt = expiresAt; }
    @PrePersist void create() { if (id == null) id = UUID.randomUUID().toString(); createdAt = LocalDateTime.now(); }
    public AppUser getUser() { return user; }
}
