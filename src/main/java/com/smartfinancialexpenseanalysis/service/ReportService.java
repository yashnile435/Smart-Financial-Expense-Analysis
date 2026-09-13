package com.smartfinancialexpenseanalysis.service;

import com.smartfinancialexpenseanalysis.dto.BudgetComparisonResponse;
import com.smartfinancialexpenseanalysis.dto.CategoryReportResponse;
import com.smartfinancialexpenseanalysis.dto.ExpenseReportItemResponse;
import com.smartfinancialexpenseanalysis.dto.FinancialReportResponse;
import com.smartfinancialexpenseanalysis.dto.PaymentMethodReportResponse;
import com.smartfinancialexpenseanalysis.dto.ReportSummaryResponse;
import com.smartfinancialexpenseanalysis.entity.Budget;
import com.smartfinancialexpenseanalysis.entity.Expense;
import com.smartfinancialexpenseanalysis.entity.PaymentMethod;
import com.smartfinancialexpenseanalysis.entity.User;
import com.smartfinancialexpenseanalysis.exception.BadRequestException;
import com.smartfinancialexpenseanalysis.exception.UnauthorizedException;
import com.smartfinancialexpenseanalysis.repository.BudgetRepository;
import com.smartfinancialexpenseanalysis.repository.ExpenseRepository;
import com.smartfinancialexpenseanalysis.repository.ExpenseSpecification;
import com.smartfinancialexpenseanalysis.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service providing financial reporting, analytics calculations,
 * category breakdowns, payment method breakdowns, and budget comparisons.
 * Uses deterministic calculations with BigDecimal arithmetic.
 */
@Service
public class ReportService {

    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;

    public ReportService(ExpenseRepository expenseRepository,
                         BudgetRepository budgetRepository,
                         UserRepository userRepository) {
        this.expenseRepository = expenseRepository;
        this.budgetRepository = budgetRepository;
        this.userRepository = userRepository;
    }

    /**
     * Generates a comprehensive monthly financial report.
     *
     * @param userEmail     authenticated user email
     * @param month         month (1 - 12)
     * @param year          calendar year (e.g. 2026)
     * @param categoryId    optional category filter
     * @param paymentMethod optional payment method filter
     * @return Complete FinancialReportResponse
     */
    @Transactional(readOnly = true)
    public FinancialReportResponse generateMonthlyReport(String userEmail, int month, int year,
                                                         Long categoryId, PaymentMethod paymentMethod) {
        validateMonthAndYear(month, year);
        User user = getUserByEmail(userEmail);

        YearMonth ym = YearMonth.of(year, month);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        String reportTitle = String.format("Monthly Financial Report - %s %d", ym.getMonth().name(), year);

        List<Expense> expenses = fetchExpenses(user.getId(), startDate, endDate, categoryId, paymentMethod);
        ReportSummaryResponse summary = calculateSummary(reportTitle, startDate, endDate, expenses);
        List<CategoryReportResponse> categoryBreakdown = calculateCategoryBreakdown(expenses, summary.getTotalExpenses());
        List<PaymentMethodReportResponse> paymentBreakdown = calculatePaymentMethodBreakdown(expenses, summary.getTotalExpenses());

        // Calculate budget comparison for the month (based on total monthly spending for the user)
        BigDecimal monthlyActualSpending = expenseRepository.sumAmountByUserIdAndDateBetween(user.getId(), startDate, endDate);
        if (monthlyActualSpending == null) {
            monthlyActualSpending = BigDecimal.ZERO;
        }
        BudgetComparisonResponse budgetComparison = buildBudgetComparison(user.getId(), month, year, monthlyActualSpending);

        List<ExpenseReportItemResponse> expenseItems = expenses.stream()
                .map(this::mapToExpenseItem)
                .collect(Collectors.toList());

        return new FinancialReportResponse(summary, categoryBreakdown, paymentBreakdown, budgetComparison, expenseItems);
    }

    /**
     * Generates a custom date-range financial report.
     *
     * @param userEmail     authenticated user email
     * @param startDate     inclusive start date
     * @param endDate       inclusive end date
     * @param categoryId    optional category filter
     * @param paymentMethod optional payment method filter
     * @return Complete FinancialReportResponse
     */
    @Transactional(readOnly = true)
    public FinancialReportResponse generateDateRangeReport(String userEmail, LocalDate startDate, LocalDate endDate,
                                                           Long categoryId, PaymentMethod paymentMethod) {
        validateDateRange(startDate, endDate);
        User user = getUserByEmail(userEmail);

        String reportTitle = String.format("Date Range Report (%s to %s)", startDate, endDate);

        List<Expense> expenses = fetchExpenses(user.getId(), startDate, endDate, categoryId, paymentMethod);
        ReportSummaryResponse summary = calculateSummary(reportTitle, startDate, endDate, expenses);
        List<CategoryReportResponse> categoryBreakdown = calculateCategoryBreakdown(expenses, summary.getTotalExpenses());
        List<PaymentMethodReportResponse> paymentBreakdown = calculatePaymentMethodBreakdown(expenses, summary.getTotalExpenses());

        // Check if date range exactly spans a single calendar month for optional budget comparison
        BudgetComparisonResponse budgetComparison = null;
        if (startDate.getDayOfMonth() == 1 && endDate.equals(YearMonth.from(startDate).atEndOfMonth())
                && startDate.getYear() == endDate.getYear() && startDate.getMonthValue() == endDate.getMonthValue()) {
            BigDecimal monthlyActualSpending = expenseRepository.sumAmountByUserIdAndDateBetween(user.getId(), startDate, endDate);
            budgetComparison = buildBudgetComparison(user.getId(), startDate.getMonthValue(), startDate.getYear(),
                    monthlyActualSpending != null ? monthlyActualSpending : BigDecimal.ZERO);
        }

        List<ExpenseReportItemResponse> expenseItems = expenses.stream()
                .map(this::mapToExpenseItem)
                .collect(Collectors.toList());

        return new FinancialReportResponse(summary, categoryBreakdown, paymentBreakdown, budgetComparison, expenseItems);
    }

    /**
     * Generates a category-wise expense breakdown report for a given date range.
     *
     * @param userEmail authenticated user email
     * @param startDate inclusive start date
     * @param endDate   inclusive end date
     * @return list of CategoryReportResponse items
     */
    @Transactional(readOnly = true)
    public List<CategoryReportResponse> getCategoryReport(String userEmail, LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        User user = getUserByEmail(userEmail);

        List<Expense> expenses = fetchExpenses(user.getId(), startDate, endDate, null, null);
        BigDecimal total = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return calculateCategoryBreakdown(expenses, total);
    }

    /**
     * Generates a payment method expense breakdown report for a given date range.
     *
     * @param userEmail authenticated user email
     * @param startDate inclusive start date
     * @param endDate   inclusive end date
     * @return list of PaymentMethodReportResponse items
     */
    @Transactional(readOnly = true)
    public List<PaymentMethodReportResponse> getPaymentMethodReport(String userEmail, LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        User user = getUserByEmail(userEmail);

        List<Expense> expenses = fetchExpenses(user.getId(), startDate, endDate, null, null);
        BigDecimal total = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return calculatePaymentMethodBreakdown(expenses, total);
    }

    /**
     * Retrieves a budget vs actual comparison for a given month and year.
     *
     * @param userEmail authenticated user email
     * @param month     month (1 - 12)
     * @param year      calendar year
     * @return BudgetComparisonResponse
     */
    @Transactional(readOnly = true)
    public BudgetComparisonResponse getBudgetComparison(String userEmail, int month, int year) {
        validateMonthAndYear(month, year);
        User user = getUserByEmail(userEmail);

        YearMonth ym = YearMonth.of(year, month);
        BigDecimal actualExpenses = expenseRepository.sumAmountByUserIdAndDateBetween(
                user.getId(), ym.atDay(1), ym.atEndOfMonth()
        );
        if (actualExpenses == null) {
            actualExpenses = BigDecimal.ZERO;
        }

        return buildBudgetComparison(user.getId(), month, year, actualExpenses);
    }

    // =========================================================================
    // Deterministic Calculation & Helper Methods
    // =========================================================================

    private List<Expense> fetchExpenses(Long userId, LocalDate startDate, LocalDate endDate,
                                        Long categoryId, PaymentMethod paymentMethod) {
        Specification<Expense> spec = ExpenseSpecification.filter(
                userId, null, categoryId, paymentMethod, startDate, endDate
        );
        Sort sort = Sort.by(Sort.Order.desc("date"), Sort.Order.desc("id"));
        return expenseRepository.findAll(spec, sort);
    }

    private ReportSummaryResponse calculateSummary(String reportTitle, LocalDate startDate, LocalDate endDate, List<Expense> expenses) {
        if (expenses == null || expenses.isEmpty()) {
            return new ReportSummaryResponse(
                    reportTitle,
                    startDate,
                    endDate,
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    0,
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            );
        }

        BigDecimal total = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        long count = expenses.size();

        BigDecimal average = total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);

        BigDecimal highest = expenses.stream()
                .map(Expense::getAmount)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal lowest = expenses.stream()
                .map(Expense::getAmount)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        return new ReportSummaryResponse(reportTitle, startDate, endDate, total, count, average, highest, lowest);
    }

    private List<CategoryReportResponse> calculateCategoryBreakdown(List<Expense> expenses, BigDecimal totalExpenses) {
        if (expenses == null || expenses.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, String> categoryNames = new LinkedHashMap<>();
        Map<Long, BigDecimal> categoryTotals = new LinkedHashMap<>();
        Map<Long, Long> categoryCounts = new LinkedHashMap<>();

        for (Expense e : expenses) {
            Long catId = e.getCategory().getId();
            String catName = e.getCategory().getName();
            categoryNames.put(catId, catName);
            categoryTotals.put(catId, categoryTotals.getOrDefault(catId, BigDecimal.ZERO).add(e.getAmount()));
            categoryCounts.put(catId, categoryCounts.getOrDefault(catId, 0L) + 1);
        }

        List<CategoryReportResponse> result = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : categoryTotals.entrySet()) {
            Long catId = entry.getKey();
            BigDecimal amount = entry.getValue().setScale(2, RoundingMode.HALF_UP);
            BigDecimal percentage = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            if (totalExpenses != null && totalExpenses.compareTo(BigDecimal.ZERO) > 0) {
                percentage = amount.multiply(BigDecimal.valueOf(100))
                        .divide(totalExpenses, 2, RoundingMode.HALF_UP);
            }
            result.add(new CategoryReportResponse(
                    catId,
                    categoryNames.get(catId),
                    amount,
                    percentage,
                    categoryCounts.get(catId)
            ));
        }

        result.sort((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()));
        return result;
    }

    private List<PaymentMethodReportResponse> calculatePaymentMethodBreakdown(List<Expense> expenses, BigDecimal totalExpenses) {
        if (expenses == null || expenses.isEmpty()) {
            return new ArrayList<>();
        }

        Map<PaymentMethod, BigDecimal> methodTotals = new LinkedHashMap<>();
        Map<PaymentMethod, Long> methodCounts = new LinkedHashMap<>();

        for (Expense e : expenses) {
            PaymentMethod pm = e.getPaymentMethod();
            methodTotals.put(pm, methodTotals.getOrDefault(pm, BigDecimal.ZERO).add(e.getAmount()));
            methodCounts.put(pm, methodCounts.getOrDefault(pm, 0L) + 1);
        }

        List<PaymentMethodReportResponse> result = new ArrayList<>();
        for (Map.Entry<PaymentMethod, BigDecimal> entry : methodTotals.entrySet()) {
            PaymentMethod pm = entry.getKey();
            BigDecimal amount = entry.getValue().setScale(2, RoundingMode.HALF_UP);
            BigDecimal percentage = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            if (totalExpenses != null && totalExpenses.compareTo(BigDecimal.ZERO) > 0) {
                percentage = amount.multiply(BigDecimal.valueOf(100))
                        .divide(totalExpenses, 2, RoundingMode.HALF_UP);
            }
            result.add(new PaymentMethodReportResponse(
                    pm,
                    amount,
                    percentage,
                    methodCounts.get(pm)
            ));
        }

        result.sort((a, b) -> b.getTotalAmount().compareTo(a.getTotalAmount()));
        return result;
    }

    private BudgetComparisonResponse buildBudgetComparison(Long userId, int month, int year, BigDecimal actualExpense) {
        BigDecimal formattedActual = actualExpense != null ? actualExpense.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        Optional<Budget> budgetOpt = budgetRepository.findByUserIdAndMonthAndYear(userId, month, year);

        if (budgetOpt.isEmpty()) {
            return new BudgetComparisonResponse(
                    month,
                    year,
                    false,
                    null,
                    formattedActual,
                    null,
                    null,
                    "NOT_CONFIGURED"
            );
        }

        Budget budget = budgetOpt.get();
        BigDecimal budgetAmount = budget.getAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal remainingAmount = budgetAmount.subtract(formattedActual).setScale(2, RoundingMode.HALF_UP);

        BigDecimal utilizationPercentage = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (budgetAmount.compareTo(BigDecimal.ZERO) > 0) {
            utilizationPercentage = formattedActual.multiply(BigDecimal.valueOf(100))
                    .divide(budgetAmount, 2, RoundingMode.HALF_UP);
        }

        String status;
        if (utilizationPercentage.compareTo(BigDecimal.valueOf(80)) < 0) {
            status = "UNDER_BUDGET";
        } else if (utilizationPercentage.compareTo(BigDecimal.valueOf(100)) <= 0) {
            status = "NEAR_LIMIT";
        } else {
            status = "OVER_BUDGET";
        }

        return new BudgetComparisonResponse(
                month,
                year,
                true,
                budgetAmount,
                formattedActual,
                remainingAmount,
                utilizationPercentage,
                status
        );
    }

    private ExpenseReportItemResponse mapToExpenseItem(Expense expense) {
        return new ExpenseReportItemResponse(
                expense.getId(),
                expense.getDate(),
                expense.getCategory() != null ? expense.getCategory().getName() : "Uncategorized",
                expense.getAmount().setScale(2, RoundingMode.HALF_UP),
                expense.getPaymentMethod(),
                expense.getDescription()
        );
    }

    private void validateMonthAndYear(int month, int year) {
        if (month < 1 || month > 12) {
            throw new BadRequestException("Month must be between 1 and 12");
        }
        if (year < 1900 || year > 2100) {
            throw new BadRequestException("Year must be between 1900 and 2100");
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BadRequestException("Start date and end date are required");
        }
        if (startDate.isAfter(endDate)) {
            throw new BadRequestException("Start date cannot be after end date");
        }
        if (startDate.getYear() < 1900 || endDate.getYear() > 2100) {
            throw new BadRequestException("Date must be between year 1900 and 2100");
        }
    }

    private User getUserByEmail(String email) {
        if (email == null) {
            throw new UnauthorizedException("User authentication required");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Authenticated user not found"));
    }
}
