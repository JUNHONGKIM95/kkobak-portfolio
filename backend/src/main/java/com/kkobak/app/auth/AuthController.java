package com.kkobak.app.auth;

import com.kkobak.app.auth.AuthDtos.LoginRequest;
import com.kkobak.app.auth.AuthDtos.LoginResponse;
import com.kkobak.app.auth.AuthDtos.SignupRequest;
import com.kkobak.app.auth.AuthDtos.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService service;
    public AuthController(AuthService service) { this.service = service; }

    @PostMapping("/signup") @ResponseStatus(HttpStatus.CREATED)
    public UserResponse signup(@Valid @RequestBody SignupRequest request) { return service.signup(request); }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) { return service.login(request.username(), request.password()); }

    @GetMapping("/me") public UserResponse me(@AuthenticationPrincipal AuthPrincipal principal) { return service.current(principal.id()); }

    @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader("Authorization") String authorization) { service.logout(authorization.substring("Bearer ".length()).trim()); }
}
