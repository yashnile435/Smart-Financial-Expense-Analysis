package com.smartfinancialexpenseanalysis.controller;

import com.smartfinancialexpenseanalysis.dto.DashboardResponse;
import com.smartfinancialexpenseanalysis.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for Financial Dashboard & Analytics.
 * Enforces session authentication and thin controller patterns.
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * Retrieves the complete financial dashboard summary for the authenticated user.
     *
     * @param authentication current security authentication
     * @return 200 OK with safe DashboardResponse
     */
    @GetMapping("/summary")
    public ResponseEntity<DashboardResponse> getDashboardSummary(Authentication authentication) {
        DashboardResponse response = dashboardService.getDashboardSummary(authentication.getName());
        return ResponseEntity.ok(response);
    }
}
