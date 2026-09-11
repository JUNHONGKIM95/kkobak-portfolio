package com.kkobak.app.report;

import java.time.LocalDate;
import java.util.List;

public final class ReportDtos {
    private ReportDtos() { }

    public record DailyCompletion(LocalDate date, long count) { }
    public record TypeSummary(String type, int total, int overdue, int dueToday) { }
    public record CycleAchievement(
            String cycleId,
            String title,
            String cycleType,
            String emoji,
            String color,
            String imageUrl,
            long completedCount,
            long onTimeCount,
            long lateCount,
            int trackedRounds,
            boolean actionRequired,
            LocalDate nextDueDate,
            Integer achievementRate) { }
    public record ReportResponse(
            int totalCycles,
            int onTrackCycles,
            int overdueCycles,
            int dueTodayCycles,
            long completedLast7Days,
            long completedLast30Days,
            int currentScore,
            int streakDays,
            List<DailyCompletion> dailyActivity,
            List<TypeSummary> typeSummary,
            List<CycleAchievement> cycleAchievements) { }
}
