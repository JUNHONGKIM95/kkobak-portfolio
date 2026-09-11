package com.kkobak.app.cycle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cycle_completions")
public class CycleCompletion {
    @Id private String id;
    @Column(name = "cycle_id", nullable = false, length = 36) private String cycleId;
    @Column(name = "owner_key", nullable = false, length = 80) private String ownerKey;
    @Column(name = "completed_date", nullable = false) private LocalDate completedDate;
    @Column(name = "previous_last_completed_date", nullable = false) private LocalDate previousLastCompletedDate;
    @Column(name = "previous_next_due_date", nullable = false) private LocalDate previousNextDueDate;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;

    protected CycleCompletion() { }

    public CycleCompletion(CycleItem cycle, LocalDate completedDate) {
        this.cycleId = cycle.getId();
        this.ownerKey = cycle.getOwnerKey();
        this.completedDate = completedDate;
        this.previousLastCompletedDate = cycle.getLastCompletedDate();
        this.previousNextDueDate = cycle.getNextDueDate();
    }

    @PrePersist void create() { id = UUID.randomUUID().toString(); createdAt = LocalDateTime.now(); }
    public String getCycleId() { return cycleId; }
    public String getOwnerKey() { return ownerKey; }
    public LocalDate getCompletedDate() { return completedDate; }
    public LocalDate getPreviousLastCompletedDate() { return previousLastCompletedDate; }
    public LocalDate getPreviousNextDueDate() { return previousNextDueDate; }
}
