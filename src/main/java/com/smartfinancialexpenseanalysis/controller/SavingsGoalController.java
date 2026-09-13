package com.smartfinancialexpenseanalysis.controller;

import com.smartfinancialexpenseanalysis.dto.ApiResponse;
import com.smartfinancialexpenseanalysis.dto.GoalContributionRequest;
import com.smartfinancialexpenseanalysis.dto.SavingsGoalRequest;
import com.smartfinancialexpenseanalysis.dto.SavingsGoalResponse;
import com.smartfinancialexpenseanalysis.service.SavingsGoalService;
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
 * REST controller for managing user savings goals and contributions.
 */
@RestController
@RequestMapping("/api/goals")
public class SavingsGoalController {

    private final SavingsGoalService savingsGoalService;

    public SavingsGoalController(SavingsGoalService savingsGoalService) {
        this.savingsGoalService = savingsGoalService;
    }

    @GetMapping
    public ResponseEntity<List<SavingsGoalResponse>> getGoals(Authentication authentication) {
        List<SavingsGoalResponse> responses = savingsGoalService.getGoals(authentication.getName());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SavingsGoalResponse> getGoalById(@PathVariable Long id, Authentication authentication) {
        SavingsGoalResponse response = savingsGoalService.getGoalById(id, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<SavingsGoalResponse> createGoal(@Valid @RequestBody SavingsGoalRequest request,
                                                          Authentication authentication) {
        SavingsGoalResponse response = savingsGoalService.createGoal(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SavingsGoalResponse> updateGoal(@PathVariable Long id,
                                                          @Valid @RequestBody SavingsGoalRequest request,
                                                          Authentication authentication) {
        SavingsGoalResponse response = savingsGoalService.updateGoal(id, request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/contributions")
    public ResponseEntity<SavingsGoalResponse> addContribution(@PathVariable Long id,
                                                               @Valid @RequestBody GoalContributionRequest request,
                                                               Authentication authentication) {
        SavingsGoalResponse response = savingsGoalService.addContribution(id, request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteGoal(@PathVariable Long id, Authentication authentication) {
        ApiResponse response = savingsGoalService.deleteGoal(id, authentication.getName());
        return ResponseEntity.ok(response);
    }
}
