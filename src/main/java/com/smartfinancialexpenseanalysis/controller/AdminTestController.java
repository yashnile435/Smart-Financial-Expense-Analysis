package com.smartfinancialexpenseanalysis.controller;

import com.smartfinancialexpenseanalysis.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller providing a protected test endpoint to verify role-based access for ADMIN users.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminTestController {

    /**
     * Protected endpoint restricted strictly to users with the ADMIN role.
     *
     * @return confirmation message if authorized
     */
    @GetMapping("/test")
    public ResponseEntity<ApiResponse> adminAccessTest() {
        return ResponseEntity.ok(new ApiResponse(true, "Welcome Admin! Role-based access verified."));
    }
}
