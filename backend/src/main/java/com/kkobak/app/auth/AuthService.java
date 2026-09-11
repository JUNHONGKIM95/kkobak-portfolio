package com.kkobak.app.auth;

import com.kkobak.app.auth.AuthDtos.LoginResponse;
import com.kkobak.app.auth.AuthDtos.SignupRequest;
import com.kkobak.app.auth.AuthDtos.StatusRequest;
import com.kkobak.app.auth.AuthDtos.UserResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private static final int SESSION_DAYS = 30;
    private final AppUserRepository users;
    private final UserSessionRepository sessions;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(AppUserRepository users, UserSessionRepository sessions, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.sessions = sessions;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse signup(SignupRequest request) {
        String username = normalizeUsername(request.username());
        if (users.existsByUsernameIgnoreCase(username)) throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다.");
        AppUser user = users.save(new AppUser(username, passwordEncoder.encode(request.password()), request.displayName().trim(), request.email().trim().toLowerCase(Locale.ROOT), "USER", "PENDING"));
        return response(user);
    }

    @Transactional
    public LoginResponse login(String username, String password) {
        AppUser user = users.findByUsernameIgnoreCase(normalizeUsername(username))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호를 확인해 주세요."));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호를 확인해 주세요.");
        if ("PENDING".equals(user.getStatus())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자 승인 대기 중입니다.");
        if (!"APPROVED".equals(user.getStatus())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "승인되지 않은 계정입니다.");

        sessions.deleteByExpiresAtBefore(LocalDateTime.now());
        String token = newToken();
        sessions.save(new UserSession(user, hash(token), LocalDateTime.now().plusDays(SESSION_DAYS)));
        user.markLogin();
        return new LoginResponse(token, response(user));
    }

    @Transactional(readOnly = true)
    public AuthPrincipal authenticate(String token) {
        AppUser user = sessions.findByTokenHashAndExpiresAtAfter(hash(token), LocalDateTime.now())
                .map(UserSession::getUser).filter(value -> "APPROVED".equals(value.getStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return new AuthPrincipal(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole());
    }

    @Transactional
    public void logout(String token) { sessions.deleteByTokenHash(hash(token)); }

    @Transactional(readOnly = true)
    public UserResponse current(String id) { return response(users.findById(id).orElseThrow()); }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() { return users.findAllByOrderByCreatedAtDesc().stream().map(this::response).toList(); }

    @Transactional
    public UserResponse updateStatus(String id, StatusRequest request) {
        if (!List.of("APPROVED", "REJECTED").contains(request.status())) throw new IllegalArgumentException("APPROVED 또는 REJECTED 상태만 사용할 수 있습니다.");
        AppUser user = users.findById(id).orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));
        if ("ADMIN".equals(user.getRole())) throw new IllegalArgumentException("관리자 상태는 변경할 수 없습니다.");
        if ("APPROVED".equals(request.status())) user.approve(); else user.reject();
        return response(user);
    }

    @Transactional
    public void ensureAdmin(String username, String password) {
        var existing = users.findByUsernameIgnoreCase(username);
        if (existing.isPresent()) {
            AppUser admin = existing.get();
            if ("ADMIN".equals(admin.getRole()) && !password.isBlank() && !passwordEncoder.matches(password, admin.getPasswordHash())) {
                admin.resetPassword(passwordEncoder.encode(password));
            }
            return;
        }
        if (password.isBlank()) throw new IllegalStateException("새 환경에서는 APP_ADMIN_PASSWORD를 반드시 설정해야 합니다.");
        AppUser admin = new AppUser(username, passwordEncoder.encode(password), "관리자", "admin@kkobak.local", "ADMIN", "APPROVED");
        admin.approve();
        users.save(admin);
    }

    private UserResponse response(AppUser user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getEmail(), user.getRole(), user.getStatus(), user.getCreatedAt(), user.getApprovedAt(), user.getLastLoginAt());
    }

    private String normalizeUsername(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); }
    private String newToken() { byte[] bytes = new byte[32]; secureRandom.nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    private String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
}
