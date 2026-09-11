package com.kkobak.app.cycle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kkobak.app.notification.PushSubscriptionRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CycleServiceTests {
    private static final String OWNER = "user-1";

    @Mock private CycleRepository cycles;
    @Mock private CycleCompletionRepository completions;
    @Mock private PushSubscriptionRepository subscriptions;
    @InjectMocks private CycleService service;

    @Test
    void listLoadsTodaysCompletionStateInOneQuery() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        CycleItem completed = cycle("cycle-1", today);
        CycleItem pending = cycle("cycle-2", today.plusDays(1));
        when(cycles.findByOwnerKeyOrderByNextDueDateAsc(OWNER)).thenReturn(List.of(completed, pending));
        when(completions.findCycleIdsByOwnerKeyAndCompletedDate(OWNER, today)).thenReturn(List.of("cycle-1"));

        List<CycleDtos.CycleResponse> result = service.list(OWNER);

        assertThat(result).extracting(CycleDtos.CycleResponse::id, CycleDtos.CycleResponse::completedToday)
                .containsExactly(tuple("cycle-1", true), tuple("cycle-2", false));
        verify(completions).findCycleIdsByOwnerKeyAndCompletedDate(OWNER, today);
        verify(completions, never()).existsByCycleIdAndOwnerKeyAndCompletedDate(any(), eq(OWNER), eq(today));
    }

    private CycleItem cycle(String id, LocalDate startDate) {
        CycleItem item = new CycleItem(OWNER, "테스트 주기", "생활", "청소", "🫧", 1, "일", startDate, null, "mint");
        ReflectionTestUtils.setField(item, "id", id);
        return item;
    }
}
