package com.smartfinancialexpenseanalysis.controller;

import com.smartfinancialexpenseanalysis.dto.ApiResponse;
import com.smartfinancialexpenseanalysis.dto.BudgetRequest;
import com.smartfinancialexpenseanalysis.dto.BudgetResponse;
import com.smartfinancialexpenseanalysis.exception.BadRequestException;
import com.smartfinancialexpenseanalysis.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller for managing user budgets and monthly spending limits.
 * Enforces authentication and thin controller patterns.
 */
@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    /**
     * Creates a new monthly budget for the authenticated user.
     *
     * @param request        budget details (month, year, amount)
     * @param authentication current security authentication
     * @return 201 Created with safe BudgetResponse
     */
    @PostMapping
    public ResponseEntity<BudgetResponse> createBudget(@Valid @RequestBody BudgetRequest request,
                                                       Authentication authentication) {
        BudgetResponse response = budgetService.createBudget(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves the authenticated user's budget for the current calendar month.
     *
     * @param authentication current security authentication
     * @return 200 OK with BudgetResponse, or 404 if no budget configured
     */
    @GetMapping("/current")
    public ResponseEntity<BudgetResponse> getCurrentMonthBudget(Authentication authentication) {
        BudgetResponse response = budgetService.getCurrentMonthBudget(authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the authenticated user's budget for a requested month and year,
     * or lists all budgets if parameters are omitted.
     *
     * @param month          calendar month (1 - 12)
     * @param year           calendar year
     * @param authentication current security authentication
     * @return 200 OK with BudgetResponse (when month & year specified) or List<BudgetResponse>
     */
    @GetMapping
    public ResponseEntity<?> getBudgets(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            Authentication authentication) {
        if (month != null || year != null) {
            if (month == null || year == null) {
                throw new BadRequestException("Both month and year parameters are required");
            }
            BudgetResponse response = budgetService.getBudgetByMonthAndYear(month, year, authentication.getName());
            return ResponseEntity.ok(response);
        }
        List<BudgetResponse> responses = budgetService.getAllBudgets(authentication.getName());
        return ResponseEntity.ok(responses);
    }

    /**
     * Retrieves all budgets belonging to the authenticated user.
     *
     * @param authentication current security authentication
     * @return 200 OK with list of BudgetResponse DTOs ordered by year DESC, month DESC
     */
    @GetMapping("/all")
    public ResponseEntity<List<BudgetResponse>> getAllBudgets(Authentication authentication) {
        List<BudgetResponse> responses = budgetService.getAllBudgets(authentication.getName());
        return ResponseEntity.ok(responses);
    }

    /**
     * Retrieves a single budget by ID, strictly verifying ownership.
     *
     * @param id             budget ID
     * @param authentication current security authentication
     * @return 200 OK with BudgetResponse, or 404 if not found / not owned
     */
    @GetMapping("/{id:[0-9]+}")
    public ResponseEntity<BudgetResponse> getBudgetById(@PathVariable Long id,
                                                        Authentication authentication) {
        BudgetResponse response = budgetService.getBudgetById(id, authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Updates an existing budget belonging to the authenticated user.
     *
     * @param id             budget ID
     * @param request        updated budget details
     * @param authentication current security authentication
     * @return 200 OK with updated BudgetResponse, or 404 if not found / not owned
     */
    @PutMapping("/{id:[0-9]+}")
    public ResponseEntity<BudgetResponse> updateBudget(@PathVariable Long id,
                                                       @Valid @RequestBody BudgetRequest request,
                                                       Authentication authentication) {
        BudgetResponse response = budgetService.updateBudget(id, request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes an existing budget belonging to the authenticated user.
     *
     * @param id             budget ID
     * @param authentication current security authentication
     * @return 200 OK with ApiResponse confirmation, or 404 if not found / not owned
     */
    @DeleteMapping("/{id:[0-9]+}")
    public ResponseEntity<ApiResponse> deleteBudget(@PathVariable Long id,
                                                    Authentication authentication) {
        ApiResponse response = budgetService.deleteBudget(id, authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves deterministic budget warning alerts for the authenticated user.
     *
     * @param month          optional month
     * @param year           optional year
     * @param authentication current security authentication
     * @return 200 OK with list of BudgetAlertResponse DTOs
     */
    @GetMapping("/alerts")
    public ResponseEntity<List<com.smartfinancialexpenseanalysis.dto.BudgetAlertResponse>> getBudgetAlerts(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            Authentication authentication) {
        List<com.smartfinancialexpenseanalysis.dto.BudgetAlertResponse> alerts =
                budgetService.getBudgetAlerts(month, year, authentication.getName());
        return ResponseEntity.ok(alerts);
    }
}
