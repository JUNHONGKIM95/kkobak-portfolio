package com.kkobak.app.cycle;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CycleItemTests {
    @Test
    void completingCycleSchedulesTheNextOccurrenceFromCompletionDate() {
        LocalDate completedDate = LocalDate.of(2026, 9, 11);
        CycleItem item = new CycleItem("user-1", "필터 교체", "주방", "교체", "✨", 3, "개월", completedDate.minusMonths(3), null, "blue");

        item.complete(completedDate);

        assertThat(item.getLastCompletedDate()).isEqualTo(completedDate);
        assertThat(item.getNextDueDate()).isEqualTo(LocalDate.of(2026, 12, 11));
    }
}
