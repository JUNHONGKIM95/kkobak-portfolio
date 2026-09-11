package com.kkobak.app.notification;

import com.kkobak.app.auth.AuthPrincipal;

import com.kkobak.app.notification.PushDtos.DispatchResponse;
import com.kkobak.app.notification.PushDtos.PublicKeyResponse;
import com.kkobak.app.notification.PushDtos.SubscriptionRequest;
import jakarta.validation.Valid;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class PushNotificationController {
    private final PushNotificationService service;
    private final String dispatchSecret;

    public PushNotificationController(PushNotificationService service, @Value("${app.notification.dispatch-secret}") String dispatchSecret) {
        this.service = service;
        this.dispatchSecret = dispatchSecret;
    }

    @GetMapping("/api/push/public-key") public PublicKeyResponse publicKey() { return new PublicKeyResponse(service.enabled(), service.publicKey()); }
    @PostMapping("/api/push/subscriptions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void subscribe(@AuthenticationPrincipal AuthPrincipal user, @Valid @RequestBody SubscriptionRequest request) {
        service.register(user.id(), request);
    }

    @PostMapping("/api/notifications/dispatch")
    public DispatchResponse dispatch(@RequestHeader(value = "X-Dispatch-Secret", defaultValue = "") String secret) {
        if (!dispatchSecret.equals(secret)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return service.dispatchDueReminders();
    }

    @PostMapping("/api/notifications/dispatch-scheduled")
    public DispatchResponse scheduledDispatch() {
        if (ZonedDateTime.now(ZoneId.of("Asia/Seoul")).getHour() != 9) return new DispatchResponse(0, 0, 0, 0);
        return service.dispatchDueReminders();
    }
}
