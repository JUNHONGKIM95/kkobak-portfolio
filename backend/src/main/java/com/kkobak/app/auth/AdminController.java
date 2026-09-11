package com.kkobak.app.auth;

import com.kkobak.app.auth.AuthDtos.StatusRequest;
import com.kkobak.app.auth.AuthDtos.UserResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminController {
    private final AuthService service;
    public AdminController(AuthService service) { this.service = service; }
    @GetMapping public List<UserResponse> list() { return service.listUsers(); }
    @PatchMapping("/{id}/status") public UserResponse status(@PathVariable String id, @Valid @RequestBody StatusRequest request) { return service.updateStatus(id, request); }
}
