package com.kkobak.app.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kkobak.app.cycle.CycleCompletion;
import com.kkobak.app.cycle.CycleCompletionRepository;
import com.kkobak.app.cycle.CycleItem;
import com.kkobak.app.cycle.CycleRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportServiceTests {
    private static final String OWNER = "user-1";

    @Mock private CycleRepository cycles;
    @Mock private CycleCompletionRepository completions;
    @InjectMocks private ReportService service;

    @Test
    void scoreExcludesDueTodayAndOverdueCycles() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        List<CycleItem> items = List.of(
                cycleStarting(today.minusDays(2)),
                cycleStarting(today.minusDays(1)),
                cycleStarting(today));
        when(cycles.findByOwnerKeyOrderByNextDueDateAsc(OWNER)).thenReturn(items);
        when(completions.findByOwnerKeyOrderByCompletedDateAsc(OWNER)).thenReturn(List.of());

        ReportDtos.ReportResponse report = service.summary(OWNER);

        assertThat(report.totalCycles()).isEqualTo(3);
        assertThat(report.onTrackCycles()).isEqualTo(1);
        assertThat(report.dueTodayCycles()).isEqualTo(1);
        assertThat(report.overdueCycles()).isEqualTo(1);
        assertThat(report.currentScore()).isEqualTo(33);
        assertThat(report.cycleAchievements()).extracting(ReportDtos.CycleAchievement::achievementRate)
                .containsExactly(0, 0, null);
        assertThat(report.typeSummary()).singleElement().satisfies(type -> {
            assertThat(type.dueToday()).isEqualTo(1);
            assertThat(type.overdue()).isEqualTo(1);
        });
    }

    @Test
    void cycleAchievementUsesOnTimeCompletionHistory() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        CycleItem item = cycleStarting(today);
        CycleCompletion onTime = completion("cycle-1", today.minusDays(2), today.minusDays(2));
        CycleCompletion late = completion("cycle-1", today.minusDays(1), today.minusDays(2));
        org.springframework.test.util.ReflectionTestUtils.setField(item, "id", "cycle-1");
        when(cycles.findByOwnerKeyOrderByNextDueDateAsc(OWNER)).thenReturn(List.of(item));
        when(completions.findByOwnerKeyOrderByCompletedDateAsc(OWNER)).thenReturn(List.of(onTime, late));

        ReportDtos.CycleAchievement achievement = service.summary(OWNER).cycleAchievements().getFirst();

        assertThat(achievement.completedCount()).isEqualTo(2);
        assertThat(achievement.onTimeCount()).isEqualTo(1);
        assertThat(achievement.lateCount()).isEqualTo(1);
        assertThat(achievement.trackedRounds()).isEqualTo(2);
        assertThat(achievement.achievementRate()).isEqualTo(50);
    }

    private CycleCompletion completion(String cycleId, LocalDate completedDate, LocalDate dueDate) {
        CycleCompletion completion = mock(CycleCompletion.class);
        when(completion.getCycleId()).thenReturn(cycleId);
        when(completion.getCompletedDate()).thenReturn(completedDate);
        when(completion.getPreviousNextDueDate()).thenReturn(dueDate);
        return completion;
    }

    private CycleItem cycleStarting(LocalDate startDate) {
        return new CycleItem(OWNER, "테스트 주기", "욕실", "청소", "🫧", 1, "일", startDate, null, "mint");
    }
}
