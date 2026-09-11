package com.kkobak.app.report;

import com.kkobak.app.cycle.CycleCompletion;
import com.kkobak.app.cycle.CycleCompletionRepository;
import com.kkobak.app.cycle.CycleItem;
import com.kkobak.app.cycle.CycleRepository;
import com.kkobak.app.report.ReportDtos.DailyCompletion;
import com.kkobak.app.report.ReportDtos.CycleAchievement;
import com.kkobak.app.report.ReportDtos.ReportResponse;
import com.kkobak.app.report.ReportDtos.TypeSummary;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Seoul");
    private final CycleRepository cycles;
    private final CycleCompletionRepository completions;

    public ReportService(CycleRepository cycles, CycleCompletionRepository completions) {
        this.cycles = cycles;
        this.completions = completions;
    }

    @Transactional(readOnly = true)
    public ReportResponse summary(String ownerKey) {
        LocalDate today = LocalDate.now(APP_ZONE);
        List<CycleItem> items = cycles.findByOwnerKeyOrderByNextDueDateAsc(ownerKey);
        List<CycleCompletion> allHistory = completions.findByOwnerKeyOrderByCompletedDateAsc(ownerKey);
        List<CycleCompletion> history = allHistory.stream()
                .filter(item -> !item.getCompletedDate().isBefore(today.minusDays(29)) && !item.getCompletedDate().isAfter(today))
                .toList();
        Map<LocalDate, Long> byDate = history.stream().collect(Collectors.groupingBy(CycleCompletion::getCompletedDate, Collectors.counting()));
        Map<String, List<CycleCompletion>> byCycle = allHistory.stream().collect(Collectors.groupingBy(CycleCompletion::getCycleId));

        int overdue = (int) items.stream().filter(item -> item.getNextDueDate().isBefore(today)).count();
        int dueToday = (int) items.stream().filter(item -> item.getNextDueDate().equals(today)).count();
        int total = items.size();
        int onTrack = total - overdue - dueToday;
        int score = total == 0 ? 0 : (int) Math.round(onTrack * 100.0 / total);
        long completedLast7 = history.stream().filter(item -> !item.getCompletedDate().isBefore(today.minusDays(6))).count();

        List<DailyCompletion> daily = IntStream.rangeClosed(0, 13)
                .mapToObj(offset -> today.minusDays(13L - offset))
                .map(date -> new DailyCompletion(date, byDate.getOrDefault(date, 0L)))
                .toList();

        Set<String> knownTypes = new LinkedHashSet<>(List.of("교체", "청소", "세탁"));
        items.stream().map(CycleItem::getCycleType).forEach(knownTypes::add);
        List<TypeSummary> types = knownTypes.stream()
                .map(type -> new TypeSummary(type,
                        (int) items.stream().filter(item -> type.equals(item.getCycleType())).count(),
                        (int) items.stream().filter(item -> type.equals(item.getCycleType()) && item.getNextDueDate().isBefore(today)).count(),
                        (int) items.stream().filter(item -> type.equals(item.getCycleType()) && item.getNextDueDate().equals(today)).count()))
                .filter(type -> type.total() > 0)
                .toList();

        List<CycleAchievement> achievements = items.stream().map(item -> {
            List<CycleCompletion> itemHistory = byCycle.getOrDefault(item.getId(), List.of());
            long completedCount = itemHistory.size();
            long onTimeCount = itemHistory.stream()
                    .filter(completion -> !completion.getCompletedDate().isAfter(completion.getPreviousNextDueDate()))
                    .count();
            boolean actionRequired = !item.getNextDueDate().isAfter(today);
            int trackedRounds = Math.toIntExact(completedCount + (actionRequired ? 1 : 0));
            Integer achievementRate = trackedRounds == 0 ? null : (int) Math.round(onTimeCount * 100.0 / trackedRounds);
            return new CycleAchievement(item.getId(), item.getTitle(), item.getCycleType(), item.getEmoji(), item.getColor(), item.getImageUrl(),
                    completedCount, onTimeCount, completedCount - onTimeCount, trackedRounds, actionRequired, item.getNextDueDate(), achievementRate);
        }).toList();

        return new ReportResponse(total, onTrack, overdue, dueToday, completedLast7, history.size(), score, streak(byDate.keySet(), today), daily, types, achievements);
    }

    private int streak(Set<LocalDate> activeDays, LocalDate today) {
        LocalDate cursor = activeDays.contains(today) ? today : today.minusDays(1);
        int streak = 0;
        while (activeDays.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }
}
