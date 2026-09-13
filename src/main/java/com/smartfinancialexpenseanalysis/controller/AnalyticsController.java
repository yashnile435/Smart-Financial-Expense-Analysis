package com.smartfinancialexpenseanalysis.controller;

import com.smartfinancialexpenseanalysis.dto.AdvancedAnalyticsResponse;
import com.smartfinancialexpenseanalysis.dto.SpendingComparisonResponse;
import com.smartfinancialexpenseanalysis.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for period spending comparisons and advanced financial analytics.
 */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/comparison")
    public ResponseEntity<SpendingComparisonResponse> compareSpending(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer comparisonMonth,
            @RequestParam(required = false) Integer comparisonYear,
            Authentication authentication) {
        SpendingComparisonResponse response = analyticsService.compareSpending(
                month, year, comparisonMonth, comparisonYear, authentication.getName()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/advanced")
    public ResponseEntity<AdvancedAnalyticsResponse> getAdvancedAnalytics(Authentication authentication) {
        AdvancedAnalyticsResponse response = analyticsService.getAdvancedAnalytics(authentication.getName());
        return ResponseEntity.ok(response);
    }
}
