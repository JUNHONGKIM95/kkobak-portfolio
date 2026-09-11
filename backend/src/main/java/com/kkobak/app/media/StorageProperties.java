package com.kkobak.app.media;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(String mode, String localRoot, String localPublicBaseUrl, String supabaseUrl, String supabaseServiceRoleKey, String supabaseBucket) {
    public StorageProperties {
        mode = mode == null ? "local" : mode;
        localRoot = localRoot == null ? "uploads" : localRoot;
        localPublicBaseUrl = localPublicBaseUrl == null ? "http://localhost:8080/uploads" : localPublicBaseUrl;
        supabaseBucket = supabaseBucket == null ? "kkobak-media" : supabaseBucket;
    }
}
