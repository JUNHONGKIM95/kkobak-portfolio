package com.kkobak.app.auth;

public record AuthPrincipal(String id, String username, String displayName, String role) { }
