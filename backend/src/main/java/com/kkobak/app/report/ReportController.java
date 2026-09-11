package com.kkobak.app.report;

import com.kkobak.app.auth.AuthPrincipal;
import com.kkobak.app.report.ReportDtos.ReportResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final ReportService service;

    public ReportController(ReportService service) { this.service = service; }

    @GetMapping("/summary")
    public ReportResponse summary(@AuthenticationPrincipal AuthPrincipal user) { return service.summary(user.id()); }
}
