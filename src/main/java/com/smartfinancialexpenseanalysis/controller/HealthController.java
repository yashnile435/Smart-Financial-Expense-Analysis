package com.smartfinancialexpenseanalysis.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for application health monitoring.
 * 
 * Provides an endpoint to verify that the backend application
 * is running and able to accept incoming HTTP requests.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    /**
     * Health check endpoint.
     * 
     * @return JSON response indicating the application status and metadata.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Smart Financial Expense Analysis");
        response.put("stage", "Production Ready (Stages 1-10 Completed)");
        response.put("message", "Application is healthy and running successfully.");
        response.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.ok(response);
    }
}
