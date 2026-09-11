package com.kkobak.app.cycle;

import com.kkobak.app.cycle.CycleDtos.CycleRequest;
import com.kkobak.app.cycle.CycleDtos.CycleResponse;
import com.kkobak.app.cycle.CycleDtos.ClaimResponse;
import com.kkobak.app.notification.PushSubscriptionRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CycleService {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Seoul");
    private final CycleRepository cycles;
    private final CycleCompletionRepository completions;
    private final PushSubscriptionRepository subscriptions;

    public CycleService(CycleRepository cycles, CycleCompletionRepository completions, PushSubscriptionRepository subscriptions) {
        this.cycles = cycles;
        this.completions = completions;
        this.subscriptions = subscriptions;
    }

    @Transactional(readOnly = true)
    public List<CycleResponse> list(String ownerKey) {
        validateOwner(ownerKey);
        LocalDate today = LocalDate.now(APP_ZONE);
        Set<String> completedToday = Set.copyOf(completions.findCycleIdsByOwnerKeyAndCompletedDate(ownerKey, today));
        return cycles.findByOwnerKeyOrderByNextDueDateAsc(ownerKey).stream()
                .map(item -> response(item, today, completedToday.contains(item.getId())))
                .toList();
    }

    @Transactional
    public CycleResponse create(String ownerKey, CycleRequest request) {
        validateUnit(request.intervalUnit());
        return response(cycles.save(new CycleItem(ownerKey, request.title(), request.category(), request.cycleType(), request.emoji(), request.intervalValue(), request.intervalUnit(), request.startDate(), request.imageUrl(), request.color())));
    }

    @Transactional
    public CycleResponse update(String ownerKey, String id, CycleRequest request) {
        validateUnit(request.intervalUnit());
        CycleItem item = owned(id, ownerKey);
        item.update(request.title(), request.category(), request.cycleType(), request.emoji(), request.intervalValue(), request.intervalUnit(), request.startDate(), request.imageUrl(), request.color());
        return response(item);
    }

    @Transactional
    public CycleResponse complete(String id, String ownerKey) {
        CycleItem item = owned(id, ownerKey);
        LocalDate today = LocalDate.now(APP_ZONE);
        if (!completions.existsByCycleIdAndOwnerKeyAndCompletedDate(id, ownerKey, today)) {
            completions.save(new CycleCompletion(item, today));
            item.complete(today);
        }
        return response(item);
    }

    @Transactional
    public CycleResponse undo(String id, String ownerKey) {
        CycleItem item = owned(id, ownerKey);
        CycleCompletion completion = completions.findFirstByCycleIdAndOwnerKeyAndCompletedDateOrderByCreatedAtDesc(id, ownerKey, LocalDate.now(APP_ZONE)).orElseThrow(() -> new NoSuchElementException("오늘 완료한 기록이 없습니다."));
        item.restore(completion.getPreviousLastCompletedDate(), completion.getPreviousNextDueDate());
        completions.delete(completion);
        return response(item);
    }

    @Transactional
    public void delete(String id, String ownerKey) {
        CycleItem item = owned(id, ownerKey);
        completions.deleteByCycleIdAndOwnerKey(id, ownerKey);
        cycles.delete(item);
    }

    @Transactional
    public ClaimResponse claim(String legacyOwnerKey, String ownerKey) {
        validateOwner(legacyOwnerKey);
        if (legacyOwnerKey.equals(ownerKey)) return new ClaimResponse(0, 0, 0);
        int completionCount = completions.reassignOwner(legacyOwnerKey, ownerKey);
        int subscriptionCount = subscriptions.reassignOwner(legacyOwnerKey, ownerKey);
        int cycleCount = cycles.reassignOwner(legacyOwnerKey, ownerKey);
        return new ClaimResponse(cycleCount, completionCount, subscriptionCount);
    }

    private CycleItem owned(String id, String ownerKey) {
        return cycles.findByIdAndOwnerKey(id, ownerKey).orElseThrow(() -> new NoSuchElementException("생활 주기를 찾을 수 없습니다."));
    }

    private CycleResponse response(CycleItem item) {
        LocalDate today = LocalDate.now(APP_ZONE);
        return response(item, today, completions.existsByCycleIdAndOwnerKeyAndCompletedDate(item.getId(), item.getOwnerKey(), today));
    }

    private CycleResponse response(CycleItem item, LocalDate today, boolean completedToday) {
        return new CycleResponse(item.getId(), item.getTitle(), item.getCategory(), item.getCycleType(), item.getEmoji(), item.getIntervalValue(), item.getIntervalUnit(), item.getStartDate(), item.getLastCompletedDate(), item.getNextDueDate(), item.getImageUrl(), item.getColor(), completedToday, ChronoUnit.DAYS.between(today, item.getNextDueDate()));
    }

    private void validateOwner(String ownerKey) { if (ownerKey == null || ownerKey.isBlank() || ownerKey.length() > 80) throw new IllegalArgumentException("올바른 사용자 키가 필요합니다."); }
    private void validateUnit(String unit) { if (!List.of("일", "주", "개월").contains(unit)) throw new IllegalArgumentException("반복 단위는 일, 주, 개월 중 하나여야 합니다."); }

}
