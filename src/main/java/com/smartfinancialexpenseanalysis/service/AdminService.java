package com.smartfinancialexpenseanalysis.service;

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
import com.smartfinancialexpenseanalysis.entity.Budget;
import com.smartfinancialexpenseanalysis.entity.BudgetStatus;
import com.smartfinancialexpenseanalysis.entity.Category;
import com.smartfinancialexpenseanalysis.entity.PaymentMethod;
import com.smartfinancialexpenseanalysis.entity.PaymentOption;
import com.smartfinancialexpenseanalysis.entity.Role;
import com.smartfinancialexpenseanalysis.entity.User;
import com.smartfinancialexpenseanalysis.exception.BadRequestException;
import com.smartfinancialexpenseanalysis.exception.DuplicateResourceException;
import com.smartfinancialexpenseanalysis.exception.ResourceNotFoundException;
import com.smartfinancialexpenseanalysis.exception.UnauthorizedException;
import com.smartfinancialexpenseanalysis.repository.BudgetRepository;
import com.smartfinancialexpenseanalysis.repository.CategoryRepository;
import com.smartfinancialexpenseanalysis.repository.ExpenseRepository;
import com.smartfinancialexpenseanalysis.repository.PaymentOptionRepository;
import com.smartfinancialexpenseanalysis.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service managing administrative operations: user management, category administration,
 * payment option master data, system-wide summaries, and analytics.
 */
@Service
public class AdminService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;
    private final PaymentOptionRepository paymentOptionRepository;

    public AdminService(UserRepository userRepository,
                        CategoryRepository categoryRepository,
                        ExpenseRepository expenseRepository,
                        BudgetRepository budgetRepository,
                        PaymentOptionRepository paymentOptionRepository) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.expenseRepository = expenseRepository;
        this.budgetRepository = budgetRepository;
        this.paymentOptionRepository = paymentOptionRepository;
    }

    // ===================================================================
    // System Overview Summary
    // ===================================================================

    @Transactional(readOnly = true)
    public AdminSummaryResponse getAdminSummary() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByEnabledTrue();
        long totalExpenses = expenseRepository.count();
        BigDecimal rawExpenseSum = expenseRepository.sumAllAmount();
        BigDecimal totalExpenseAmount = (rawExpenseSum != null ? rawExpenseSum : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        YearMonth currentYm = YearMonth.now();
        LocalDate startOfMonth = currentYm.atDay(1);
        LocalDate endOfMonth = currentYm.atEndOfMonth();
        BigDecimal rawMonthSum = expenseRepository.sumAmountByDateBetween(startOfMonth, endOfMonth);
        BigDecimal currentMonthExpenseAmount = (rawMonthSum != null ? rawMonthSum : BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        long totalBudgets = budgetRepository.count();
        BigDecimal rawBudgetSum = budgetRepository.sumAllAmount();
        BigDecimal totalBudgetAmount = (rawBudgetSum != null ? rawBudgetSum : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        return new AdminSummaryResponse(
                totalUsers,
                activeUsers,
                totalExpenses,
                totalExpenseAmount,
                currentMonthExpenseAmount,
                totalBudgets,
                totalBudgetAmount
        );
    }

    // ===================================================================
    // User Management
    // ===================================================================

    @Transactional(readOnly = true)
    public List<AdminUserResponse> listUsers(String search) {
        List<User> users;
        if (search != null && !search.trim().isEmpty()) {
            String term = search.trim();
            users = userRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByIdDesc(term, term);
        } else {
            users = userRepository.findAllByOrderByIdDesc();
        }

        return users.stream()
                .map(this::toAdminUserResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
        return toAdminUserResponse(user);
    }

    @Transactional
    public AdminUserResponse updateUserStatus(Long targetUserId, boolean enabled, String currentAdminEmail) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + targetUserId));

        User currentAdmin = userRepository.findByEmail(currentAdminEmail)
                .orElseThrow(() -> new UnauthorizedException("Authenticated administrator not found"));

        // Safety Rule 1: Cannot disable own account
        if (targetUser.getId().equals(currentAdmin.getId()) && !enabled) {
            throw new BadRequestException("Administrators cannot disable their own account");
        }

        // Safety Rule 2: Cannot disable the last remaining active administrator
        if (!enabled && targetUser.getRole() == Role.ADMIN) {
            long activeAdminCount = userRepository.countByRoleAndEnabledTrue(Role.ADMIN);
            if (activeAdminCount <= 1) {
                throw new BadRequestException("Cannot disable the last remaining active administrator");
            }
        }

        targetUser.setEnabled(enabled);
        User saved = userRepository.save(targetUser);
        return toAdminUserResponse(saved);
    }

    @Transactional
    public AdminUserResponse updateUserRole(Long targetUserId, Role newRole, String currentAdminEmail) {
        if (newRole == null) {
            throw new BadRequestException("Role cannot be null");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + targetUserId));

        User currentAdmin = userRepository.findByEmail(currentAdminEmail)
                .orElseThrow(() -> new UnauthorizedException("Authenticated administrator not found"));

        // Safety Rule 1: Cannot demote own account
        if (targetUser.getId().equals(currentAdmin.getId()) && newRole != Role.ADMIN) {
            throw new BadRequestException("Administrators cannot demote their own account");
        }

        // Safety Rule 2: Cannot demote the last remaining active administrator
        if (targetUser.getRole() == Role.ADMIN && newRole != Role.ADMIN) {
            long activeAdminCount = userRepository.countByRoleAndEnabledTrue(Role.ADMIN);
            if (activeAdminCount <= 1) {
                throw new BadRequestException("Cannot demote the last remaining active administrator");
            }
        }

        targetUser.setRole(newRole);
        User saved = userRepository.save(targetUser);
        return toAdminUserResponse(saved);
    }

    private AdminUserResponse toAdminUserResponse(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled()
        );
    }

    // ===================================================================
    // Category Management
    // ===================================================================

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories() {
        return categoryRepository.findAll(Sort.by("id"))
                .stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName(), c.getDescription()))
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        String trimmedName = request.getName().trim();
        if (trimmedName.isEmpty()) {
            throw new BadRequestException("Category name cannot be blank");
        }
        if (categoryRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new DuplicateResourceException("Category with name '" + trimmedName + "' already exists");
        }

        String description = request.getDescription() != null ? request.getDescription().trim() : null;
        Category category = new Category(trimmedName, description);
        Category saved = categoryRepository.save(category);
        return new CategoryResponse(saved.getId(), saved.getName(), saved.getDescription());
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));

        String trimmedName = request.getName().trim();
        if (trimmedName.isEmpty()) {
            throw new BadRequestException("Category name cannot be blank");
        }
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(trimmedName, id)) {
            throw new DuplicateResourceException("Category with name '" + trimmedName + "' already exists");
        }

        category.setName(trimmedName);
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription().trim());
        }
        Category updated = categoryRepository.save(category);
        return new CategoryResponse(updated.getId(), updated.getName(), updated.getDescription());
    }

    @Transactional
    public ApiResponse deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));

        // Delete Safety Check: Cannot delete category referenced by expenses
        if (expenseRepository.existsByCategoryId(id)) {
            throw new DuplicateResourceException("Category cannot be deleted because expenses are using it.");
        }

        categoryRepository.delete(category);
        return new ApiResponse(true, "Category deleted successfully");
    }

    // ===================================================================
    // Payment Option Management
    // ===================================================================

    @Transactional(readOnly = true)
    public List<PaymentOptionResponse> listPaymentOptions() {
        return paymentOptionRepository.findAllByOrderByIdAsc()
                .stream()
                .map(po -> new PaymentOptionResponse(
                        po.getId(),
                        po.getName(),
                        po.getDescription(),
                        po.isActive(),
                        po.getCreatedAt(),
                        po.getUpdatedAt()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public PaymentOptionResponse createPaymentOption(PaymentOptionRequest request) {
        String trimmedName = request.getName() != null ? request.getName().trim() : "";
        if (trimmedName.isEmpty()) {
            throw new BadRequestException("Payment option name cannot be blank");
        }
        if (paymentOptionRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new DuplicateResourceException("Payment option with name '" + trimmedName + "' already exists");
        }

        String description = request.getDescription() != null ? request.getDescription().trim() : null;
        boolean active = request.getActive() != null ? request.getActive() : true;

        PaymentOption option = new PaymentOption(trimmedName, description, active);
        PaymentOption saved = paymentOptionRepository.save(option);
        return new PaymentOptionResponse(saved.getId(), saved.getName(), saved.getDescription(), saved.isActive(), saved.getCreatedAt(), saved.getUpdatedAt());
    }

    @Transactional
    public PaymentOptionResponse updatePaymentOption(Long id, PaymentOptionRequest request) {
        PaymentOption option = paymentOptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment option not found with ID: " + id));

        String trimmedName = request.getName() != null ? request.getName().trim() : "";
        if (trimmedName.isEmpty()) {
            throw new BadRequestException("Payment option name cannot be blank");
        }
        if (paymentOptionRepository.existsByNameIgnoreCaseAndIdNot(trimmedName, id)) {
            throw new DuplicateResourceException("Payment option with name '" + trimmedName + "' already exists");
        }

        option.setName(trimmedName);
        if (request.getDescription() != null) {
            option.setDescription(request.getDescription().trim());
        }
        if (request.getActive() != null) {
            option.setActive(request.getActive());
        }

        PaymentOption updated = paymentOptionRepository.save(option);
        return new PaymentOptionResponse(updated.getId(), updated.getName(), updated.getDescription(), updated.isActive(), updated.getCreatedAt(), updated.getUpdatedAt());
    }

    @Transactional
    public PaymentOptionResponse togglePaymentOptionStatus(Long id, Boolean active) {
        PaymentOption option = paymentOptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment option not found with ID: " + id));

        boolean newActive = (active != null) ? active : !option.isActive();
        option.setActive(newActive);
        PaymentOption updated = paymentOptionRepository.save(option);
        return new PaymentOptionResponse(updated.getId(), updated.getName(), updated.getDescription(), updated.isActive(), updated.getCreatedAt(), updated.getUpdatedAt());
    }

    @Transactional
    public ApiResponse deletePaymentOption(Long id) {
        PaymentOption option = paymentOptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment option not found with ID: " + id));

        // Referential integrity check: If referenced by expenses, safely deactivate instead of deleting
        boolean isReferenced = expenseRepository.existsByPaymentMethod(PaymentMethod.of(option.getName()));
        if (isReferenced) {
            option.setActive(false);
            paymentOptionRepository.save(option);
            return new ApiResponse(true, "Payment option is referenced by existing transactions and has been deactivated instead of permanently deleted.");
        }

        paymentOptionRepository.delete(option);
        return new ApiResponse(true, "Payment option deleted successfully");
    }

    // ===================================================================
    // Financial Overviews
    // ===================================================================

    @Transactional(readOnly = true)
    public AdminExpenseSummaryResponse getExpenseSummary() {
        BigDecimal rawExpenseSum = expenseRepository.sumAllAmount();
        BigDecimal totalExpenseAmount = (rawExpenseSum != null ? rawExpenseSum : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        long expenseCount = expenseRepository.count();

        BigDecimal averageExpense = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (expenseCount > 0) {
            averageExpense = totalExpenseAmount.divide(BigDecimal.valueOf(expenseCount), 2, RoundingMode.HALF_UP);
        }

        BigDecimal rawMax = expenseRepository.findMaxAmountSystemWide();
        BigDecimal highestExpense = (rawMax != null ? rawMax : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        YearMonth currentYm = YearMonth.now();
        BigDecimal rawCurrentMonthSum = expenseRepository.sumAmountByDateBetween(
                currentYm.atDay(1), currentYm.atEndOfMonth()
        );
        BigDecimal currentMonthExpenseAmount = (rawCurrentMonthSum != null ? rawCurrentMonthSum : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        return new AdminExpenseSummaryResponse(
                totalExpenseAmount,
                expenseCount,
                averageExpense,
                highestExpense,
                currentMonthExpenseAmount
        );
    }

    @Transactional(readOnly = true)
    public AdminBudgetSummaryResponse getBudgetSummary() {
        long totalBudgets = budgetRepository.count();
        BigDecimal rawBudgetSum = budgetRepository.sumAllAmount();
        BigDecimal totalBudgetAmount = (rawBudgetSum != null ? rawBudgetSum : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        BigDecimal averageBudgetAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (totalBudgets > 0) {
            averageBudgetAmount = totalBudgetAmount.divide(BigDecimal.valueOf(totalBudgets), 2, RoundingMode.HALF_UP);
        }

        List<Budget> budgets = budgetRepository.findAll();
        long underBudget = 0;
        long nearLimit = 0;
        long overBudget = 0;

        for (Budget b : budgets) {
            YearMonth ym = YearMonth.of(b.getYear(), b.getMonth());
            BigDecimal actual = expenseRepository.sumAmountByUserIdAndDateBetween(
                    b.getUser().getId(), ym.atDay(1), ym.atEndOfMonth()
            );
            if (actual == null) {
                actual = BigDecimal.ZERO;
            }

            BigDecimal utilization = BigDecimal.ZERO;
            if (b.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                utilization = actual.multiply(BigDecimal.valueOf(100)).divide(b.getAmount(), 2, RoundingMode.HALF_UP);
            }

            BudgetStatus status = BudgetStatus.fromUtilization(utilization);
            if (status == BudgetStatus.UNDER_BUDGET) {
                underBudget++;
            } else if (status == BudgetStatus.NEAR_LIMIT) {
                nearLimit++;
            } else if (status == BudgetStatus.OVER_BUDGET) {
                overBudget++;
            }
        }

        return new AdminBudgetSummaryResponse(
                totalBudgets,
                totalBudgetAmount,
                averageBudgetAmount,
                underBudget,
                nearLimit,
                overBudget
        );
    }

    // ===================================================================
    // Analytics
    // ===================================================================

    @Transactional(readOnly = true)
    public List<AdminCategoryAnalyticsResponse> getCategoryAnalytics() {
        BigDecimal rawTotal = expenseRepository.sumAllAmount();
        BigDecimal systemTotal = (rawTotal != null) ? rawTotal : BigDecimal.ZERO;
        List<Object[]> rows = expenseRepository.sumAmountAndCountGroupByCategory();

        List<AdminCategoryAnalyticsResponse> result = new ArrayList<>();
        for (Object[] row : rows) {
            Long categoryId = (Long) row[0];
            String categoryName = (String) row[1];
            BigDecimal amount = ((BigDecimal) row[2]).setScale(2, RoundingMode.HALF_UP);
            Long count = (Long) row[3];

            BigDecimal percentage = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            if (systemTotal.compareTo(BigDecimal.ZERO) > 0) {
                percentage = amount.multiply(BigDecimal.valueOf(100)).divide(systemTotal, 2, RoundingMode.HALF_UP);
            }

            result.add(new AdminCategoryAnalyticsResponse(categoryId, categoryName, amount, count, percentage));
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<AdminPaymentMethodAnalyticsResponse> getPaymentMethodAnalytics() {
        BigDecimal rawTotal = expenseRepository.sumAllAmount();
        BigDecimal systemTotal = (rawTotal != null) ? rawTotal : BigDecimal.ZERO;
        List<Object[]> rows = expenseRepository.sumAmountAndCountGroupByPaymentMethod();

        List<AdminPaymentMethodAnalyticsResponse> result = new ArrayList<>();
        for (Object[] row : rows) {
            PaymentMethod paymentMethod = (PaymentMethod) row[0];
            BigDecimal amount = ((BigDecimal) row[1]).setScale(2, RoundingMode.HALF_UP);
            Long count = (Long) row[2];

            BigDecimal percentage = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            if (systemTotal.compareTo(BigDecimal.ZERO) > 0) {
                percentage = amount.multiply(BigDecimal.valueOf(100)).divide(systemTotal, 2, RoundingMode.HALF_UP);
            }

            result.add(new AdminPaymentMethodAnalyticsResponse(paymentMethod, amount, count, percentage));
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<AdminMonthlyAnalyticsResponse> getMonthlyAnalytics(int monthsCount) {
        if (monthsCount <= 0) {
            monthsCount = 6;
        }

        YearMonth currentYm = YearMonth.now();
        YearMonth startYm = currentYm.minusMonths(monthsCount - 1);
        LocalDate startDate = startYm.atDay(1);

        List<Object[]> dbRows = expenseRepository.sumAmountAndCountByMonthSince(startDate);

        // Map key: "year-month" -> [BigDecimal total, Long count]
        Map<String, Object[]> dataMap = new HashMap<>();
        for (Object[] row : dbRows) {
            Integer year = (Integer) row[0];
            Integer month = (Integer) row[1];
            BigDecimal amount = (BigDecimal) row[2];
            Long count = (Long) row[3];
            dataMap.put(year + "-" + month, new Object[]{amount, count});
        }

        List<AdminMonthlyAnalyticsResponse> result = new ArrayList<>();
        for (int i = 0; i < monthsCount; i++) {
            YearMonth target = startYm.plusMonths(i);
            String key = target.getYear() + "-" + target.getMonthValue();
            String monthName = target.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);

            BigDecimal amount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            long count = 0L;

            if (dataMap.containsKey(key)) {
                Object[] data = dataMap.get(key);
                amount = ((BigDecimal) data[0]).setScale(2, RoundingMode.HALF_UP);
                count = (Long) data[1];
            }

            result.add(new AdminMonthlyAnalyticsResponse(
                    target.getYear(),
                    target.getMonthValue(),
                    monthName,
                    amount,
                    count
            ));
        }

        return result;
    }
}
