package com.smartfinancialexpenseanalysis.controller;

import com.smartfinancialexpenseanalysis.dto.ApiResponse;
import com.smartfinancialexpenseanalysis.dto.RecurringExpenseRequest;
import com.smartfinancialexpenseanalysis.dto.RecurringExpenseResponse;
import com.smartfinancialexpenseanalysis.service.RecurringExpenseService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for managing scheduled recurring expenses.
 */
@RestController
@RequestMapping("/api/recurring-expenses")
public class RecurringExpenseController {

    private final RecurringExpenseService recurringExpenseService;

    public RecurringExpenseController(RecurringExpenseService recurringExpenseService) {
        this.recurringExpenseService = recurringExpenseService;
    }

    @GetMapping
    public ResponseEntity<List<RecurringExpenseResponse>> getRecurringExpenses(Authentication authentication) {
        List<RecurringExpenseResponse> responses = recurringExpenseService.getRecurringExpenses(authentication.getName());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecurringExpenseResponse> getRecurringExpenseById(@PathVariable Long id,
                                                                            Authentication authentication) {
        RecurringExpenseResponse response = recurringExpenseService.getRecurringExpenseById(id, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<RecurringExpenseResponse> createRecurringExpense(@Valid @RequestBody RecurringExpenseRequest request,
                                                                           Authentication authentication) {
        RecurringExpenseResponse response = recurringExpenseService.createRecurringExpense(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecurringExpenseResponse> updateRecurringExpense(@PathVariable Long id,
                                                                           @Valid @RequestBody RecurringExpenseRequest request,
                                                                           Authentication authentication) {
        RecurringExpenseResponse response = recurringExpenseService.updateRecurringExpense(id, request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<RecurringExpenseResponse> toggleStatus(@PathVariable Long id,
                                                                 Authentication authentication) {
        RecurringExpenseResponse response = recurringExpenseService.toggleStatus(id, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteRecurringExpense(@PathVariable Long id,
                                                              Authentication authentication) {
        ApiResponse response = recurringExpenseService.deleteRecurringExpense(id, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/generate")
    public ResponseEntity<RecurringExpenseResponse> generateDueExpense(@PathVariable Long id,
                                                                       Authentication authentication) {
        RecurringExpenseResponse response = recurringExpenseService.generateDueExpense(id, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/process-due")
    public ResponseEntity<List<RecurringExpenseResponse>> processAllDueExpenses(Authentication authentication) {
        List<RecurringExpenseResponse> responses = recurringExpenseService.processAllDueExpenses(authentication.getName());
        return ResponseEntity.ok(responses);
    }
}
