package com.kkobak.app.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kkobak.app.cycle.CycleItem;
import com.kkobak.app.cycle.CycleRepository;
import com.kkobak.app.notification.PushDtos.DispatchResponse;
import com.kkobak.app.notification.PushDtos.SubscriptionRequest;
import java.security.Security;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import nl.martijndwars.webpush.Encoding;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PushNotificationService {
    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Seoul");
    private final PushSubscriptionRepository subscriptions;
    private final CycleRepository cycles;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String publicKey;
    private final String privateKey;
    private final String subject;
    private final String frontendUrl;

    public PushNotificationService(PushSubscriptionRepository subscriptions, CycleRepository cycles,
            @Value("${app.push.vapid.public-key:}") String publicKey,
            @Value("${app.push.vapid.private-key:}") String privateKey,
            @Value("${app.push.vapid.subject:mailto:hello@kkobak.app}") String subject,
            @Value("${app.frontend-url:http://localhost:3000}") String frontendUrl) {
        this.subscriptions = subscriptions;
        this.cycles = cycles;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.subject = subject;
        this.frontendUrl = frontendUrl;
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) Security.addProvider(new BouncyCastleProvider());
    }

    public boolean enabled() { return !publicKey.isBlank() && !privateKey.isBlank(); }
    public String publicKey() { return publicKey; }

    @Transactional
    public void register(String ownerKey, SubscriptionRequest request) {
        if (request.keys() == null) throw new IllegalArgumentException("푸시 구독 키가 필요합니다.");
        PushSubscription subscription = subscriptions.findByEndpoint(request.endpoint()).orElseGet(() -> new PushSubscription(ownerKey, request.endpoint(), request.keys().p256dh(), request.keys().auth()));
        subscription.update(ownerKey, request.endpoint(), request.keys().p256dh(), request.keys().auth());
        subscriptions.save(subscription);
    }

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void scheduledDispatch() { dispatchDueReminders(); }

    @Transactional
    public DispatchResponse dispatchDueReminders() {
        if (!enabled()) return new DispatchResponse(0, 0, 0, 0);
        LocalDate today = LocalDate.now(APP_ZONE);
        Map<String, List<CycleItem>> byOwner = cycles.findByNextDueDateLessThanEqual(today).stream().collect(Collectors.groupingBy(CycleItem::getOwnerKey));
        int subscriptionCount = 0;
        int delivered = 0;
        int failed = 0;
        for (Map.Entry<String, List<CycleItem>> entry : byOwner.entrySet()) {
            String body = message(entry.getValue());
            String payload = payload("오늘도 꼬박 챙겨봐요!", body, frontendUrl, "kkobak-" + today);
            for (PushSubscription subscription : subscriptions.findByOwnerKey(entry.getKey())) {
                if (today.equals(subscription.getLastNotifiedDate())) continue;
                subscriptionCount++;
                if (send(subscription, payload)) {
                    subscription.markNotified(today);
                    delivered++;
                } else failed++;
            }
        }
        return new DispatchResponse(byOwner.size(), subscriptionCount, delivered, failed);
    }

    private String message(List<CycleItem> due) {
        if (due.size() == 1) return due.getFirst().getTitle() + " 예정일이에요.";
        return due.getFirst().getTitle() + " 외 " + (due.size() - 1) + "개를 챙길 날이에요.";
    }

    private boolean send(PushSubscription subscription, String payload) {
        try {
            PushService service = new PushService(publicKey, privateKey, subject);
            HttpResponse response = service.send(new Notification(subscription.getEndpoint(), subscription.getP256dh(), subscription.getAuth(), payload), Encoding.AES128GCM);
            int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) return true;
            if (status == 404 || status == 410) subscriptions.delete(subscription);
            log.warn("Push delivery returned HTTP {}", status);
        } catch (Exception exception) {
            log.warn("Push delivery failed", exception);
        }
        return false;
    }

    private String payload(String title, String body, String url, String tag) {
        try {
            return objectMapper.writeValueAsString(Map.of("title", title, "body", body, "url", url, "tag", tag, "icon", "/icons/icon-192.png", "badge", "/icons/icon-192.png"));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("푸시 메시지를 만들지 못했습니다.", exception);
        }
    }
}
