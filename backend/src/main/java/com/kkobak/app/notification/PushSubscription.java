package com.kkobak.app.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "push_subscriptions")
public class PushSubscription {
    @Id private String id;
    @Column(name = "owner_key", nullable = false, length = 80) private String ownerKey;
    @Column(nullable = false, unique = true, length = 2048) private String endpoint;
    @Column(nullable = false, length = 255) private String p256dh;
    @Column(nullable = false, length = 255) private String auth;
    @Column(name = "last_notified_date") private LocalDate lastNotifiedDate;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    protected PushSubscription() { }
    public PushSubscription(String ownerKey, String endpoint, String p256dh, String auth) { update(ownerKey, endpoint, p256dh, auth); }
    public void update(String ownerKey, String endpoint, String p256dh, String auth) { this.ownerKey = ownerKey; this.endpoint = endpoint; this.p256dh = p256dh; this.auth = auth; }
    public void markNotified(LocalDate date) { this.lastNotifiedDate = date; }
    @PrePersist void create() { id = UUID.randomUUID().toString(); createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate void updateTimestamp() { updatedAt = LocalDateTime.now(); }
    public String getOwnerKey() { return ownerKey; }
    public String getEndpoint() { return endpoint; }
    public String getP256dh() { return p256dh; }
    public String getAuth() { return auth; }
    public LocalDate getLastNotifiedDate() { return lastNotifiedDate; }
}
