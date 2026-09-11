package com.kkobak.app.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public final class AuthDtos {
    private AuthDtos() { }

    public record SignupRequest(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9._-]{3,30}", message = "아이디는 영문, 숫자, 점, 밑줄, 하이픈으로 3~30자여야 합니다.") String username,
            @NotBlank @Size(min = 8, max = 72, message = "비밀번호는 8자 이상이어야 합니다.") String password,
            @NotBlank @Size(max = 80) String displayName,
            @NotBlank @Email @Size(max = 255) String email) { }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) { }
    public record StatusRequest(@NotBlank String status) { }
    public record UserResponse(String id, String username, String displayName, String email, String role, String status,
                               LocalDateTime createdAt, LocalDateTime approvedAt, LocalDateTime lastLoginAt) { }
    public record LoginResponse(String token, UserResponse user) { }
}
