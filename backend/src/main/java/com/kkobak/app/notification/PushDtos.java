package com.kkobak.app.notification;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public final class PushDtos {
    private PushDtos() { }
    public record PushKeys(@NotBlank String p256dh, @NotBlank String auth) { }
    public record SubscriptionRequest(@NotBlank String endpoint, Long expirationTime, @Valid PushKeys keys) { }
    public record PublicKeyResponse(boolean enabled, String publicKey) { }
    public record DispatchResponse(int owners, int subscriptions, int delivered, int failed) { }
}
