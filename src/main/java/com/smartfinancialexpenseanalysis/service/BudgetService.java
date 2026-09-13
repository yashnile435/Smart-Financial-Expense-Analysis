package com.smartfinancialexpenseanalysis.service;

import com.smartfinancialexpenseanalysis.dto.ApiResponse;
import com.smartfinancialexpenseanalysis.dto.BudgetRequest;
import com.smartfinancialexpenseanalysis.dto.BudgetResponse;
import com.smartfinancialexpenseanalysis.entity.Budget;
import com.smartfinancialexpenseanalysis.entity.BudgetStatus;
import com.smartfinancialexpenseanalysis.entity.User;
import com.smartfinancialexpenseanalysis.exception.BadRequestException;
import com.smartfinancialexpenseanalysis.exception.DuplicateResourceException;
import com.smartfinancialexpenseanalysis.exception.ResourceNotFoundException;
import com.smartfinancialexpenseanalysis.exception.UnauthorizedException;
import com.smartfinancialexpenseanalysis.repository.BudgetRepository;
import com.smartfinancialexpenseanalysis.repository.ExpenseRepository;
import com.smartfinancialexpenseanalysis.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service managing monthly spending budgets and deterministic financial metrics.
 *
 * Enforces strict user data ownership and provides calculated budget utilization
 * with exact arithmetic calculations.
 */
@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    public BudgetService(BudgetRepository budgetRepository,
                         ExpenseRepository expenseRepository,
                         UserRepository userRepository) {
        this.budgetRepository = budgetRepository;
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
    }

    /**
     * Creates a new monthly budget for the authenticated user.
     *
     * @param request   validated budget creation parameters
     * @param userEmail email of the authenticated user
     * @return populated BudgetResponse DTO
     */
    @Transactional
    public BudgetResponse createBudget(BudgetRequest request, String userEmail) {
        User user = getUserByEmail(userEmail);
        validateBudgetFields(request.getMonth(), request.getYear(), request.getAmount());

        if (budgetRepository.findByUserIdAndMonthAndYear(user.getId(), request.getMonth(), request.getYear()).isPresent()) {
            throw new DuplicateResourceException("A budget already exists for " + request.getMonth() + "/" + request.getYear());
        }

        Budget budget = new Budget(
                user,
                request.getMonth(),
                request.getYear(),
                request.getAmount()
        );

        Budget saved = budgetRepository.save(budget);
        return buildBudgetResponse(saved, user.getId());
    }

    /**
     * Retrieves the authenticated user's budget for the current calendar month and year.
     *
     * @param userEmail email of the authenticated user
     * @return populated BudgetResponse DTO
     */
    @Transactional(readOnly = true)
    public BudgetResponse getCurrentMonthBudget(String userEmail) {
        User user = getUserByEmail(userEmail);
        LocalDate now = LocalDate.now();
        int month = now.getMonthValue();
        int year = now.getYear();

        Budget budget = budgetRepository.findByUserIdAndMonthAndYear(user.getId(), month, year)
                .orElseThrow(() -> new ResourceNotFoundException("No budget configured for current month (" + month + "/" + year + ")"));

        return buildBudgetResponse(budget, user.getId());
    }

    /**
     * Retrieves the authenticated user's budget for a specified month and year.
     *
     * @param month     calendar month (1 - 12)
     * @param year      calendar year
     * @param userEmail email of the authenticated user
     * @return populated BudgetResponse DTO
     */
    @Transactional(readOnly = true)
    public BudgetResponse getBudgetByMonthAndYear(Integer month, Integer year, String userEmail) {
        User user = getUserByEmail(userEmail);
        validateMonthAndYear(month, year);

        Budget budget = budgetRepository.findByUserIdAndMonthAndYear(user.getId(), month, year)
                .orElseThrow(() -> new ResourceNotFoundException("No budget found for " + month + "/" + year));

        return buildBudgetResponse(budget, user.getId());
    }

    /**
     * Retrieves all budgets belonging to the authenticated user, ordered by year DESC and month DESC.
     *
     * @param userEmail email of the authenticated user
     * @return list of populated BudgetResponse DTOs
     */
    @Transactional(readOnly = true)
    public List<BudgetResponse> getAllBudgets(String userEmail) {
        User user = getUserByEmail(userEmail);
        List<Budget> budgets = budgetRepository.findByUserIdOrderByYearDescMonthDesc(user.getId());

        return budgets.stream()
                .map(budget -> buildBudgetResponse(budget, user.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a single budget by its ID, enforcing ownership.
     *
     * @param id        budget ID
     * @param userEmail email of the authenticated user
     * @return populated BudgetResponse DTO
     */
    @Transactional(readOnly = true)
    public BudgetResponse getBudgetById(Long id, String userEmail) {
        User user = getUserByEmail(userEmail);
        Budget budget = budgetRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with ID: " + id));

        return buildBudgetResponse(budget, user.getId());
    }

    /**
     * Updates an existing budget belonging to the authenticated user.
     *
     * @param id        budget ID
     * @param request   validated budget update parameters
     * @param userEmail email of the authenticated user
     * @return populated BudgetResponse DTO
     */
    @Transactional
    public BudgetResponse updateBudget(Long id, BudgetRequest request, String userEmail) {
        User user = getUserByEmail(userEmail);
        validateBudgetFields(request.getMonth(), request.getYear(), request.getAmount());

        Budget existing = budgetRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with ID: " + id));

        Optional<Budget> duplicate = budgetRepository.findByUserIdAndMonthAndYear(user.getId(), request.getMonth(), request.getYear());
        if (duplicate.isPresent() && !duplicate.get().getId().equals(id)) {
            throw new DuplicateResourceException("A budget already exists for " + request.getMonth() + "/" + request.getYear());
        }

        existing.setMonth(request.getMonth());
        existing.setYear(request.getYear());
        existing.setAmount(request.getAmount());

        Budget updated = budgetRepository.save(existing);
        return buildBudgetResponse(updated, user.getId());
    }

    /**
     * Deletes an existing budget belonging to the authenticated user.
     *
     * @param id        budget ID
     * @param userEmail email of the authenticated user
     * @return success ApiResponse
     */
    @Transactional
    public ApiResponse deleteBudget(Long id, String userEmail) {
        User user = getUserByEmail(userEmail);
        Budget existing = budgetRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with ID: " + id));

        budgetRepository.delete(existing);
        return new ApiResponse(true, "Budget deleted successfully");
    }

    /**
     * Computes the deterministic budget vs expense metrics:
     * - Total Expenses (from database SUM between start and end of month)
     * - Remaining Amount = Budget Amount - Total Expenses
     * - Utilization Percentage = (Total Expenses / Budget Amount) * 100
     * - Budget Status (UNDER_BUDGET, NEAR_LIMIT, OVER_BUDGET)
     */
    private BudgetResponse buildBudgetResponse(Budget budget, Long userId) {
        YearMonth ym = YearMonth.of(budget.getYear(), budget.getMonth());
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        BigDecimal totalExpenses = expenseRepository.sumAmountByUserIdAndDateBetween(userId, startDate, endDate);
        if (totalExpenses == null) {
            totalExpenses = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else {
            totalExpenses = totalExpenses.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal budgetAmount = budget.getAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal remainingAmount = budgetAmount.subtract(totalExpenses).setScale(2, RoundingMode.HALF_UP);

        BigDecimal utilizationPercentage;
        if (budgetAmount.compareTo(BigDecimal.ZERO) > 0) {
            utilizationPercentage = totalExpenses
                    .multiply(BigDecimal.valueOf(100))
                    .divide(budgetAmount, 2, RoundingMode.HALF_UP);
        } else {
            utilizationPercentage = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BudgetStatus status = BudgetStatus.fromUtilization(utilizationPercentage);

        return new BudgetResponse(
                budget.getId(),
                budget.getMonth(),
                budget.getYear(),
                budgetAmount,
                totalExpenses,
                remainingAmount,
                utilizationPercentage,
                status
        );
    }

    /**
     * Retrieves deterministic budget warning alerts for the authenticated user.
     *
     * @param month     optional month
     * @param year      optional year
     * @param userEmail email of authenticated user
     * @return list of BudgetAlertResponse DTOs
     */
    @Transactional(readOnly = true)
    public List<com.smartfinancialexpenseanalysis.dto.BudgetAlertResponse> getBudgetAlerts(Integer month, Integer year, String userEmail) {
        User user = getUserByEmail(userEmail);
        List<Budget> budgets;
        if (month != null && year != null) {
            validateMonthAndYear(month, year);
            budgets = budgetRepository.findByUserIdAndMonthAndYear(user.getId(), month, year)
                    .map(List::of)
                    .orElse(List.of());
        } else {
            budgets = budgetRepository.findByUserIdOrderByYearDescMonthDesc(user.getId());
        }

        return budgets.stream()
                .map(b -> buildBudgetAlertResponse(b, user.getId()))
                .collect(Collectors.toList());
    }

    private com.smartfinancialexpenseanalysis.dto.BudgetAlertResponse buildBudgetAlertResponse(Budget budget, Long userId) {
        BudgetResponse br = buildBudgetResponse(budget, userId);
        com.smartfinancialexpenseanalysis.dto.BudgetAlertLevel level = com.smartfinancialexpenseanalysis.dto.BudgetAlertLevel.fromUtilization(br.getUtilizationPercentage());
        String message;
        switch (level) {
            case NORMAL -> message = String.format("You have used %s%% of your monthly budget.", br.getUtilizationPercentage());
            case WARNING -> message = String.format("You have used %s%% of your monthly budget. Please monitor your spending.", br.getUtilizationPercentage());
            case CRITICAL_WARNING -> message = String.format("Your spending is approaching the monthly budget limit (%s%% used).", br.getUtilizationPercentage());
            case EXCEEDED -> message = String.format("Your monthly budget has been exceeded by ₹%s (%s%% used).",
                    br.getTotalExpenses().subtract(br.getBudgetAmount()).abs(), br.getUtilizationPercentage());
            default -> message = "Budget status normal.";
        }

        return new com.smartfinancialexpenseanalysis.dto.BudgetAlertResponse(
                budget.getId(),
                budget.getMonth(),
                budget.getYear(),
                br.getBudgetAmount(),
                br.getTotalExpenses(),
                br.getRemainingAmount(),
                br.getUtilizationPercentage(),
                level,
                message
        );
    }

    private User getUserByEmail(String email) {
        if (email == null || email.trim().isEmpty() || "anonymousUser".equalsIgnoreCase(email)) {
            throw new UnauthorizedException("User is not authenticated");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
    }

    private void validateMonthAndYear(Integer month, Integer year) {
        if (month == null || month < 1 || month > 12) {
            throw new BadRequestException("Month must be between 1 and 12");
        }
        if (year == null || year < 2000) {
            throw new BadRequestException("Year must be 2000 or later");
        }
    }

    private void validateBudgetFields(Integer month, Integer year, BigDecimal amount) {
        validateMonthAndYear(month, year);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Amount must be greater than zero");
        }
    }
}
