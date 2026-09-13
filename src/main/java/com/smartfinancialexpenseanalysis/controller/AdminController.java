package com.smartfinancialexpenseanalysis.controller;

import com.smartfinancialexpenseanalysis.dto.AdminBudgetSummaryResponse;
import com.smartfinancialexpenseanalysis.dto.AdminCategoryAnalyticsResponse;
import com.smartfinancialexpenseanalysis.dto.AdminExpenseSummaryResponse;
import com.smartfinancialexpenseanalysis.dto.AdminMonthlyAnalyticsResponse;
import com.smartfinancialexpenseanalysis.dto.AdminPaymentMethodAnalyticsResponse;
import com.smartfinancialexpenseanalysis.dto.AdminSummaryResponse;
import com.smartfinancialexpenseanalysis.dto.AdminUserResponse;
import com.smartfinancialexpenseanalysis.dto.ApiResponse;
import com.smartfinancialexpenseanalysis.dto.CategoryRequest;
import com.smartfinancialexpenseanalysis.dto.CategoryResponse;
import com.smartfinancialexpenseanalysis.dto.PaymentOptionRequest;
import com.smartfinancialexpenseanalysis.dto.PaymentOptionResponse;
import com.smartfinancialexpenseanalysis.dto.UpdateUserRoleRequest;
import com.smartfinancialexpenseanalysis.dto.UpdateUserStatusRequest;
import com.smartfinancialexpenseanalysis.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller exposing administrative management and reporting endpoints.
 * Strictly protected for users with ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // ===================================================================
    // System Overview
    // ===================================================================

    @GetMapping("/summary")
    public ResponseEntity<AdminSummaryResponse> getAdminSummary() {
        return ResponseEntity.ok(adminService.getAdminSummary());
    }

    // ===================================================================
    // User Management
    // ===================================================================

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserResponse>> listUsers(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(adminService.listUsers(search));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<AdminUserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getUserById(id));
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<AdminUserResponse> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(adminService.updateUserStatus(id, request.getEnabled(), authentication.getName()));
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<AdminUserResponse> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(adminService.updateUserRole(id, request.getRole(), authentication.getName()));
    }

    // ===================================================================
    // Category Management
    // ===================================================================

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponse>> listCategories() {
        return ResponseEntity.ok(adminService.listCategories());
    }

    @PostMapping("/categories")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = adminService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(adminService.updateCategory(id, request));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<ApiResponse> deleteCategory(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.deleteCategory(id));
    }

    // ===================================================================
    // Payment Option Management
    // ===================================================================

    @GetMapping("/payment-options")
    public ResponseEntity<List<PaymentOptionResponse>> listPaymentOptions() {
        return ResponseEntity.ok(adminService.listPaymentOptions());
    }

    @PostMapping("/payment-options")
    public ResponseEntity<PaymentOptionResponse> createPaymentOption(@Valid @RequestBody PaymentOptionRequest request) {
        PaymentOptionResponse response = adminService.createPaymentOption(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/payment-options/{id}")
    public ResponseEntity<PaymentOptionResponse> updatePaymentOption(
            @PathVariable Long id,
            @Valid @RequestBody PaymentOptionRequest request) {
        return ResponseEntity.ok(adminService.updatePaymentOption(id, request));
    }

    @PatchMapping("/payment-options/{id}/status")
    public ResponseEntity<PaymentOptionResponse> togglePaymentOptionStatus(
            @PathVariable Long id,
            @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(adminService.togglePaymentOptionStatus(id, active));
    }

    @DeleteMapping("/payment-options/{id}")
    public ResponseEntity<ApiResponse> deletePaymentOption(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.deletePaymentOption(id));
    }

    // ===================================================================
    // System-Wide Financial Overviews
    // ===================================================================

    @GetMapping("/expenses/summary")
    public ResponseEntity<AdminExpenseSummaryResponse> getExpenseSummary() {
        return ResponseEntity.ok(adminService.getExpenseSummary());
    }

    @GetMapping("/budgets/summary")
    public ResponseEntity<AdminBudgetSummaryResponse> getBudgetSummary() {
        return ResponseEntity.ok(adminService.getBudgetSummary());
    }

    // ===================================================================
    // Analytics
    // ===================================================================

    @GetMapping("/analytics/categories")
    public ResponseEntity<List<AdminCategoryAnalyticsResponse>> getCategoryAnalytics() {
        return ResponseEntity.ok(adminService.getCategoryAnalytics());
    }

    @GetMapping("/analytics/payment-methods")
    public ResponseEntity<List<AdminPaymentMethodAnalyticsResponse>> getPaymentMethodAnalytics() {
        return ResponseEntity.ok(adminService.getPaymentMethodAnalytics());
    }

    @GetMapping("/analytics/monthly")
    public ResponseEntity<List<AdminMonthlyAnalyticsResponse>> getMonthlyAnalytics(
            @RequestParam(defaultValue = "6") int months) {
        return ResponseEntity.ok(adminService.getMonthlyAnalytics(months));
    }
}
