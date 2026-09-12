package com.smartfinancialexpenseanalysis.controller;

import com.smartfinancialexpenseanalysis.dto.ApiResponse;
import com.smartfinancialexpenseanalysis.dto.ExpenseRequest;
import com.smartfinancialexpenseanalysis.dto.ExpenseResponse;
import com.smartfinancialexpenseanalysis.entity.PaymentMethod;
import com.smartfinancialexpenseanalysis.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;

/**
 * REST Controller for managing expenses.
 * Enforces authentication and thin controller patterns.
 */
@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    /**
     * Creates a new expense for the authenticated user.
     *
     * @param request        expense payload
     * @param authentication current security authentication
     * @return 201 Created with safe ExpenseResponse
     */
    @PostMapping
    public ResponseEntity<ExpenseResponse> createExpense(@Valid @RequestBody ExpenseRequest request,
                                                         Authentication authentication) {
        ExpenseResponse response = expenseService.createExpense(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves expenses belonging to the authenticated user with optional search and filters.
     *
     * @param search         optional text search on description
     * @param categoryId     optional category filter
     * @param paymentMethod  optional payment method filter
     * @param startDate      optional start date filter (ISO format)
     * @param endDate        optional end date filter (ISO format)
     * @param page           optional page index
     * @param size           optional page size
     * @param authentication current security authentication
     * @return 200 OK with list of expenses
     */
    @GetMapping
    public ResponseEntity<List<ExpenseResponse>> getExpenses(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Authentication authentication) {
        List<ExpenseResponse> responses = expenseService.getExpenses(
                authentication.getName(), search, categoryId, paymentMethod, startDate, endDate, page, size);
        return ResponseEntity.ok(responses);
    }

    /**
     * Retrieves a single expense by ID, strictly verifying ownership.
     *
     * @param id             expense ID
     * @param authentication current security authentication
     * @return 200 OK with ExpenseResponse or 404 if not found / not owned
     */
    @GetMapping("/{id}")
    public ResponseEntity<ExpenseResponse> getExpenseById(@PathVariable Long id,
                                                          Authentication authentication) {
        ExpenseResponse response = expenseService.getExpenseById(id, authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Updates an existing expense belonging to the authenticated user.
     *
     * @param id             expense ID
     * @param request        updated fields
     * @param authentication current security authentication
     * @return 200 OK with updated ExpenseResponse or 404 if not found / not owned
     */
    @PutMapping("/{id}")
    public ResponseEntity<ExpenseResponse> updateExpense(@PathVariable Long id,
                                                         @Valid @RequestBody ExpenseRequest request,
                                                         Authentication authentication) {
        ExpenseResponse response = expenseService.updateExpense(id, request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes an expense belonging to the authenticated user.
     *
     * @param id             expense ID
     * @param authentication current security authentication
     * @return 200 OK confirmation or 404 if not found / not owned
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteExpense(@PathVariable Long id,
                                                     Authentication authentication) {
        ApiResponse response = expenseService.deleteExpense(id, authentication.getName());
        return ResponseEntity.ok(response);
    }
}
