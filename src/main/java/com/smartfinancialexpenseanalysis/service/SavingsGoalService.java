package com.smartfinancialexpenseanalysis.service;

import com.smartfinancialexpenseanalysis.dto.ApiResponse;
import com.smartfinancialexpenseanalysis.dto.GoalContributionRequest;
import com.smartfinancialexpenseanalysis.dto.SavingsGoalRequest;
import com.smartfinancialexpenseanalysis.dto.SavingsGoalResponse;
import com.smartfinancialexpenseanalysis.entity.SavingsGoal;
import com.smartfinancialexpenseanalysis.entity.SavingsGoalStatus;
import com.smartfinancialexpenseanalysis.entity.User;
import com.smartfinancialexpenseanalysis.exception.BadRequestException;
import com.smartfinancialexpenseanalysis.exception.ResourceNotFoundException;
import com.smartfinancialexpenseanalysis.exception.UnauthorizedException;
import com.smartfinancialexpenseanalysis.repository.SavingsGoalRepository;
import com.smartfinancialexpenseanalysis.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service managing user savings goals and contributions with strict ownership verification.
 */
@Service
public class SavingsGoalService {

    private final SavingsGoalRepository savingsGoalRepository;
    private final UserRepository userRepository;

    public SavingsGoalService(SavingsGoalRepository savingsGoalRepository,
                              UserRepository userRepository) {
        this.savingsGoalRepository = savingsGoalRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<SavingsGoalResponse> getGoals(String userEmail) {
        User user = getUserByEmail(userEmail);
        return savingsGoalRepository.findByUserIdOrderByTargetDateAsc(user.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SavingsGoalResponse getGoalById(Long id, String userEmail) {
        User user = getUserByEmail(userEmail);
        SavingsGoal goal = savingsGoalRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Savings goal not found with ID: " + id));
        return mapToResponse(goal);
    }

    @Transactional
    public SavingsGoalResponse createGoal(SavingsGoalRequest request, String userEmail) {
        User user = getUserByEmail(userEmail);

        if (request.getTargetAmount() == null || request.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Target amount must be greater than zero");
        }

        BigDecimal current = request.getCurrentAmount() != null ? request.getCurrentAmount() : BigDecimal.ZERO;
        if (current.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Current saved amount cannot be negative");
        }

        SavingsGoalStatus status = current.compareTo(request.getTargetAmount()) >= 0
                ? SavingsGoalStatus.COMPLETED
                : SavingsGoalStatus.ACTIVE;

        SavingsGoal goal = new SavingsGoal(
                user,
                request.getName().trim(),
                request.getDescription() != null ? request.getDescription().trim() : null,
                request.getTargetAmount(),
                current,
                request.getTargetDate(),
                status
        );

        SavingsGoal saved = savingsGoalRepository.save(goal);
        return mapToResponse(saved);
    }

    @Transactional
    public SavingsGoalResponse updateGoal(Long id, SavingsGoalRequest request, String userEmail) {
        User user = getUserByEmail(userEmail);
        SavingsGoal goal = savingsGoalRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Savings goal not found with ID: " + id));

        if (request.getTargetAmount() == null || request.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Target amount must be greater than zero");
        }

        BigDecimal current = request.getCurrentAmount() != null ? request.getCurrentAmount() : goal.getCurrentAmount();
        if (current.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Current saved amount cannot be negative");
        }

        goal.setName(request.getName().trim());
        goal.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        goal.setTargetAmount(request.getTargetAmount());
        goal.setCurrentAmount(current);
        goal.setTargetDate(request.getTargetDate());

        if (goal.getStatus() != SavingsGoalStatus.CANCELLED) {
            goal.setStatus(current.compareTo(request.getTargetAmount()) >= 0
                    ? SavingsGoalStatus.COMPLETED
                    : SavingsGoalStatus.ACTIVE);
        }

        SavingsGoal updated = savingsGoalRepository.save(goal);
        return mapToResponse(updated);
    }

    @Transactional
    public SavingsGoalResponse addContribution(Long id, GoalContributionRequest request, String userEmail) {
        User user = getUserByEmail(userEmail);
        SavingsGoal goal = savingsGoalRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Savings goal not found with ID: " + id));

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Contribution amount must be greater than zero");
        }

        if (goal.getStatus() == SavingsGoalStatus.CANCELLED) {
            throw new BadRequestException("Cannot add contributions to a cancelled goal");
        }

        BigDecimal newTotal = goal.getCurrentAmount().add(request.getAmount());
        goal.setCurrentAmount(newTotal);

        if (newTotal.compareTo(goal.getTargetAmount()) >= 0) {
            goal.setStatus(SavingsGoalStatus.COMPLETED);
        }

        SavingsGoal updated = savingsGoalRepository.save(goal);
        return mapToResponse(updated);
    }

    @Transactional
    public ApiResponse deleteGoal(Long id, String userEmail) {
        User user = getUserByEmail(userEmail);
        SavingsGoal goal = savingsGoalRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Savings goal not found with ID: " + id));

        savingsGoalRepository.delete(goal);
        return new ApiResponse(true, "Savings goal deleted successfully");
    }

    private SavingsGoalResponse mapToResponse(SavingsGoal goal) {
        BigDecimal target = goal.getTargetAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal current = goal.getCurrentAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal remaining = target.subtract(current).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        BigDecimal progress = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (target.compareTo(BigDecimal.ZERO) > 0) {
            progress = current.multiply(BigDecimal.valueOf(100)).divide(target, 2, RoundingMode.HALF_UP);
        }

        return new SavingsGoalResponse(
                goal.getId(),
                goal.getName(),
                goal.getDescription(),
                target,
                current,
                remaining,
                progress,
                goal.getTargetDate(),
                goal.getStatus(),
                goal.getCreatedAt(),
                goal.getUpdatedAt()
        );
    }

    private User getUserByEmail(String email) {
        if (email == null || email.trim().isEmpty() || "anonymousUser".equalsIgnoreCase(email)) {
            throw new UnauthorizedException("User is not authenticated");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
    }
}
