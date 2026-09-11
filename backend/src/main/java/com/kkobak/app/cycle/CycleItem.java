package com.kkobak.app.cycle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cycle_items")
public class CycleItem {
    @Id private String id;
    @Column(name = "owner_key", nullable = false, length = 80) private String ownerKey;
    @Column(nullable = false, length = 120) private String title;
    @Column(nullable = false, length = 40) private String category;
    @Column(name = "cycle_type", nullable = false, length = 20) private String cycleType;
    @Column(nullable = false, length = 20) private String emoji;
    @Column(name = "interval_value", nullable = false) private int intervalValue;
    @Column(name = "interval_unit", nullable = false, length = 20) private String intervalUnit;
    @Column(name = "start_date", nullable = false) private LocalDate startDate;
    @Column(name = "last_completed_date", nullable = false) private LocalDate lastCompletedDate;
    @Column(name = "next_due_date", nullable = false) private LocalDate nextDueDate;
    @Column(name = "image_url", length = 2048) private String imageUrl;
    @Column(nullable = false, length = 30) private String color;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    protected CycleItem() { }

    public CycleItem(String ownerKey, String title, String category, String cycleType, String emoji, int intervalValue, String intervalUnit, LocalDate startDate, String imageUrl, String color) {
        this.ownerKey = ownerKey;
        this.startDate = startDate;
        this.lastCompletedDate = startDate;
        this.intervalValue = intervalValue;
        this.intervalUnit = intervalUnit;
        this.nextDueDate = nextDate(startDate);
        update(title, category, cycleType, emoji, intervalValue, intervalUnit, startDate, imageUrl, color);
    }

    public void update(String title, String category, String cycleType, String emoji, int intervalValue, String intervalUnit, LocalDate startDate, String imageUrl, String color) {
        boolean scheduleChanged = this.startDate == null || !this.startDate.equals(startDate) || this.intervalValue != intervalValue || !intervalUnit.equals(this.intervalUnit);
        this.title = title;
        this.category = category;
        this.cycleType = cycleType;
        this.emoji = emoji;
        this.intervalValue = intervalValue;
        this.intervalUnit = intervalUnit;
        this.startDate = startDate;
        if (scheduleChanged) {
            this.lastCompletedDate = startDate;
            this.nextDueDate = nextDate(startDate);
        }
        this.imageUrl = imageUrl;
        this.color = color;
    }

    public void complete(LocalDate completedDate) {
        this.lastCompletedDate = completedDate;
        this.nextDueDate = nextDate(completedDate);
    }

    public void restore(LocalDate previousLast, LocalDate previousNext) {
        this.lastCompletedDate = previousLast;
        this.nextDueDate = previousNext;
    }

    @PrePersist
    void create() {
        if (id == null) id = UUID.randomUUID().toString();
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate void updateTimestamp() { updatedAt = LocalDateTime.now(); }

    public String getId() { return id; }
    public String getOwnerKey() { return ownerKey; }
    public String getTitle() { return title; }
    public String getCategory() { return category; }
    public String getCycleType() { return cycleType; }
    public String getEmoji() { return emoji; }
    public int getIntervalValue() { return intervalValue; }
    public String getIntervalUnit() { return intervalUnit; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getLastCompletedDate() { return lastCompletedDate; }
    public LocalDate getNextDueDate() { return nextDueDate; }
    public String getImageUrl() { return imageUrl; }
    public String getColor() { return color; }

    private LocalDate nextDate(LocalDate base) {
        return switch (intervalUnit) {
            case "일" -> base.plusDays(intervalValue);
            case "주" -> base.plusWeeks(intervalValue);
            case "개월" -> base.plusMonths(intervalValue);
            default -> throw new IllegalArgumentException("지원하지 않는 반복 단위입니다.");
        };
    }
}
