package com.kkobak.app.auth;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AdminInitializer implements ApplicationRunner {
    private final AuthService service;
    private final String username;
    private final String password;

    public AdminInitializer(AuthService service,
            @Value("${app.admin.username:admin}") String username,
            @Value("${app.admin.password:}") String password) {
        this.service = service;
        this.username = username;
        this.password = password;
    }

    @Override public void run(ApplicationArguments args) { service.ensureAdmin(username, password); }
}
