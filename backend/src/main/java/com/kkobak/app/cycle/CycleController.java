package com.kkobak.app.cycle;

import com.kkobak.app.cycle.CycleDtos.CycleRequest;
import com.kkobak.app.cycle.CycleDtos.CycleResponse;
import com.kkobak.app.cycle.CycleDtos.ClaimRequest;
import com.kkobak.app.cycle.CycleDtos.ClaimResponse;
import com.kkobak.app.auth.AuthPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cycles")
public class CycleController {
    private final CycleService service;
    public CycleController(CycleService service) { this.service = service; }

    @GetMapping public List<CycleResponse> list(@AuthenticationPrincipal AuthPrincipal user) { return service.list(user.id()); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public CycleResponse create(@AuthenticationPrincipal AuthPrincipal user, @Valid @RequestBody CycleRequest request) { return service.create(user.id(), request); }
    @PutMapping("/{id}") public CycleResponse update(@AuthenticationPrincipal AuthPrincipal user, @PathVariable String id, @Valid @RequestBody CycleRequest request) { return service.update(user.id(), id, request); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@AuthenticationPrincipal AuthPrincipal user, @PathVariable String id) { service.delete(id, user.id()); }
    @PostMapping("/{id}/complete") public CycleResponse complete(@AuthenticationPrincipal AuthPrincipal user, @PathVariable String id) { return service.complete(id, user.id()); }
    @DeleteMapping("/{id}/complete") public CycleResponse undo(@AuthenticationPrincipal AuthPrincipal user, @PathVariable String id) { return service.undo(id, user.id()); }
    @PostMapping("/{id}/end") public CycleResponse end(@AuthenticationPrincipal AuthPrincipal user, @PathVariable String id) { return service.end(id, user.id()); }
    @DeleteMapping("/{id}/end") public CycleResponse reopen(@AuthenticationPrincipal AuthPrincipal user, @PathVariable String id) { return service.reopen(id, user.id()); }
    @PostMapping("/claim") public ClaimResponse claim(@AuthenticationPrincipal AuthPrincipal user, @Valid @RequestBody ClaimRequest request) { return service.claim(request.ownerKey(), user.id()); }
}
