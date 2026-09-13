package com.smartfinancialexpenseanalysis.service;

import com.smartfinancialexpenseanalysis.dto.CategoryExpenseResponse;
import com.smartfinancialexpenseanalysis.dto.DashboardResponse;
import com.smartfinancialexpenseanalysis.dto.InsightResponse;
import com.smartfinancialexpenseanalysis.dto.MonthlyExpenseResponse;
import com.smartfinancialexpenseanalysis.dto.PaymentMethodExpenseResponse;
import com.smartfinancialexpenseanalysis.dto.RecentExpenseResponse;
import com.smartfinancialexpenseanalysis.entity.Budget;
import com.smartfinancialexpenseanalysis.entity.BudgetStatus;
import com.smartfinancialexpenseanalysis.entity.Expense;
import com.smartfinancialexpenseanalysis.entity.PaymentMethod;
import com.smartfinancialexpenseanalysis.entity.User;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service managing financial dashboard aggregations, monthly trends,
 * and rule-based financial health insights.
 */
@Service
public class DashboardService {

    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;

    public DashboardService(ExpenseRepository expenseRepository,
                            BudgetRepository budgetRepository,
                            UserRepository userRepository) {
        this.expenseRepository = expenseRepository;
        this.budgetRepository = budgetRepository;
        this.userRepository = userRepository;
    }

    /**
     * Aggregates and returns the complete dashboard summary for the authenticated user.
     *
     * @param userEmail email of the authenticated user
     * @return DashboardResponse with all summary metrics, breakdowns, trends, and rule-based insights
     */
    @Transactional(readOnly = true)
    public DashboardResponse getDashboardSummary(String userEmail) {
        User user = getUserByEmail(userEmail);
        Long userId = user.getId();

        YearMonth currentYm = YearMonth.now();
        int currentMonth = currentYm.getMonthValue();
        int currentYear = currentYm.getYear();
        LocalDate currentMonthStart = currentYm.atDay(1);
        LocalDate currentMonthEnd = currentYm.atEndOfMonth();

        // 1. Total Expenses & Current Month Expenses
        BigDecimal totalExpenses = expenseRepository.sumAmountByUserId(userId);
        totalExpenses = totalExpenses != null ? totalExpenses.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal currentMonthExpenses = expenseRepository.sumAmountByUserIdAndDateBetween(userId, currentMonthStart, currentMonthEnd);
        currentMonthExpenses = currentMonthExpenses != null ? currentMonthExpenses.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        // 2. Expense Counts
        long totalExpenseCount = expenseRepository.countByUserId(userId);
        long currentMonthExpenseCount = expenseRepository.countByUserIdAndDateBetween(userId, currentMonthStart, currentMonthEnd);

        // 3. Average Expense
        BigDecimal averageExpense;
        if (totalExpenseCount > 0) {
            averageExpense = totalExpenses.divide(BigDecimal.valueOf(totalExpenseCount), 2, RoundingMode.HALF_UP);
        } else {
            averageExpense = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        // 4. Highest Expense
        BigDecimal highestExpense = expenseRepository.findMaxAmountByUserId(userId);
        highestExpense = highestExpense != null ? highestExpense.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        // 5. Current Month Budget Metrics
        Optional<Budget> budgetOpt = budgetRepository.findByUserIdAndMonthAndYear(userId, currentMonth, currentYear);
        BigDecimal currentMonthBudget = null;
        BigDecimal currentMonthRemaining = null;
        BigDecimal currentMonthUtilization = null;
        String currentMonthBudgetStatus = null;

        if (budgetOpt.isPresent()) {
            Budget budget = budgetOpt.get();
            currentMonthBudget = budget.getAmount().setScale(2, RoundingMode.HALF_UP);
            currentMonthRemaining = currentMonthBudget.subtract(currentMonthExpenses).setScale(2, RoundingMode.HALF_UP);
            if (currentMonthBudget.compareTo(BigDecimal.ZERO) > 0) {
                currentMonthUtilization = currentMonthExpenses
                        .multiply(BigDecimal.valueOf(100))
                        .divide(currentMonthBudget, 2, RoundingMode.HALF_UP);
            } else {
                currentMonthUtilization = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }
            currentMonthBudgetStatus = BudgetStatus.fromUtilization(currentMonthUtilization).name();
        }

        // 6. Category Breakdown
        List<Object[]> catRows = expenseRepository.sumAmountByUserIdGroupByCategory(userId);
        List<CategoryExpenseResponse> categoryBreakdown = new ArrayList<>();
        for (Object[] row : catRows) {
            Long catId = (Long) row[0];
            String catName = (String) row[1];
            BigDecimal amount = ((BigDecimal) row[2]).setScale(2, RoundingMode.HALF_UP);
            BigDecimal percentage = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            if (totalExpenses.compareTo(BigDecimal.ZERO) > 0) {
                percentage = amount.multiply(BigDecimal.valueOf(100)).divide(totalExpenses, 2, RoundingMode.HALF_UP);
            }
            categoryBreakdown.add(new CategoryExpenseResponse(catId, catName, amount, percentage));
        }

        // 7. Payment Method Breakdown
        List<Object[]> pmRows = expenseRepository.sumAmountByUserIdGroupByPaymentMethod(userId);
        List<PaymentMethodExpenseResponse> paymentMethodBreakdown = new ArrayList<>();
        for (Object[] row : pmRows) {
            PaymentMethod pm = (PaymentMethod) row[0];
            BigDecimal amount = ((BigDecimal) row[1]).setScale(2, RoundingMode.HALF_UP);
            BigDecimal percentage = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            if (totalExpenses.compareTo(BigDecimal.ZERO) > 0) {
                percentage = amount.multiply(BigDecimal.valueOf(100)).divide(totalExpenses, 2, RoundingMode.HALF_UP);
            }
            paymentMethodBreakdown.add(new PaymentMethodExpenseResponse(pm, amount, percentage));
        }

        // 8. Monthly Spending Trend (Last 6 consecutive months including current)
        List<MonthlyExpenseResponse> monthlyTrend = new ArrayList<>();
        YearMonth startYm = currentYm.minusMonths(5);
        for (int i = 0; i < 6; i++) {
            YearMonth ym = startYm.plusMonths(i);
            LocalDate start = ym.atDay(1);
            LocalDate end = ym.atEndOfMonth();
            BigDecimal monthSpend = expenseRepository.sumAmountByUserIdAndDateBetween(userId, start, end);
            monthSpend = monthSpend != null ? monthSpend.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            monthlyTrend.add(new MonthlyExpenseResponse(ym.getMonthValue(), ym.getYear(), monthSpend));
        }

        // 9. Recent Expenses (5 most recent)
        List<Expense> recentExpensesEntities = expenseRepository.findTop5ByUserIdOrderByDateDescIdDesc(userId);
        List<RecentExpenseResponse> recentExpenses = recentExpensesEntities.stream()
                .map(e -> new RecentExpenseResponse(
                        e.getId(),
                        e.getAmount().setScale(2, RoundingMode.HALF_UP),
                        e.getCategory().getName(),
                        e.getPaymentMethod(),
                        e.getDate(),
                        e.getDescription()
                ))
                .collect(Collectors.toList());

        // 10. Deterministic Rule-Based Financial Insights
        List<InsightResponse> insights = generateInsights(
                totalExpenseCount,
                totalExpenses,
                currentMonthExpenses,
                currentMonthBudget,
                currentMonthUtilization,
                categoryBreakdown,
                currentYm,
                userId
        );

        return new DashboardResponse(
                currentMonth,
                currentYear,
                totalExpenses,
                currentMonthExpenses,
                totalExpenseCount,
                currentMonthExpenseCount,
                averageExpense,
                highestExpense,
                currentMonthBudget,
                currentMonthRemaining,
                currentMonthUtilization,
                currentMonthBudgetStatus,
                categoryBreakdown,
                paymentMethodBreakdown,
                monthlyTrend,
                recentExpenses,
                insights
        );
    }

    /**
     * Generates rule-based financial insights using simple, deterministic conditionals.
     */
    private List<InsightResponse> generateInsights(long expenseCount,
                                                   BigDecimal totalExpenses,
                                                   BigDecimal currentMonthExpenses,
                                                   BigDecimal currentMonthBudget,
                                                   BigDecimal currentMonthUtilization,
                                                   List<CategoryExpenseResponse> categoryBreakdown,
                                                   YearMonth currentYm,
                                                   Long userId) {
        List<InsightResponse> insights = new ArrayList<>();

        // Rule 7: No expenses yet
        if (expenseCount == 0) {
            insights.add(new InsightResponse(
                    "NO_DATA",
                    "No Expenses Yet",
                    "Add your expenses to start analyzing your financial activity."
            ));
            return insights;
        }

        // Budget Rules (Rules 1, 2, 3)
        if (currentMonthBudget != null && currentMonthUtilization != null) {
            if (currentMonthUtilization.compareTo(BigDecimal.valueOf(100)) > 0) {
                insights.add(new InsightResponse(
                        "OVER_BUDGET",
                        "Budget Exceeded",
                        "Your expenses have exceeded your current monthly budget."
                ));
            } else if (currentMonthUtilization.compareTo(BigDecimal.valueOf(80)) >= 0) {
                insights.add(new InsightResponse(
                        "NEAR_LIMIT",
                        "Budget Near Limit",
                        "You are close to reaching your monthly budget."
                ));
            } else {
                insights.add(new InsightResponse(
                        "UNDER_BUDGET",
                        "Budget Under Control",
                        "Your current spending is below 80% of your monthly budget."
                ));
            }
        }

        // Rule 4: Category Concentration (> 50% in one category)
        if (totalExpenses.compareTo(BigDecimal.ZERO) > 0) {
            boolean hasConcentration = categoryBreakdown.stream()
                    .anyMatch(cat -> cat.getPercentage().compareTo(BigDecimal.valueOf(50)) > 0);
            if (hasConcentration) {
                insights.add(new InsightResponse(
                        "CATEGORY_CONCENTRATION",
                        "High Category Spending",
                        "More than half of your spending is concentrated in one category."
                ));
            }
        }

        // Rules 5 & 6: Month-over-Month Spending Comparison
        YearMonth prevYm = currentYm.minusMonths(1);
        BigDecimal prevMonthExpenses = expenseRepository.sumAmountByUserIdAndDateBetween(
                userId, prevYm.atDay(1), prevYm.atEndOfMonth());
        if (prevMonthExpenses == null) {
            prevMonthExpenses = BigDecimal.ZERO;
        }

        if (currentMonthExpenses.compareTo(prevMonthExpenses) > 0) {
            insights.add(new InsightResponse(
                    "SPENDING_INCREASED",
                    "Spending Increased",
                    "Your spending increased compared with the previous month."
            ));
        } else if (currentMonthExpenses.compareTo(prevMonthExpenses) < 0 && prevMonthExpenses.compareTo(BigDecimal.ZERO) > 0) {
            insights.add(new InsightResponse(
                    "SPENDING_REDUCED",
                    "Spending Reduced",
                    "Your spending decreased compared with the previous month."
            ));
        }

        return insights;
    }

    private User getUserByEmail(String email) {
        if (email == null || email.trim().isEmpty() || "anonymousUser".equalsIgnoreCase(email)) {
            throw new UnauthorizedException("User is not authenticated");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
    }
}
