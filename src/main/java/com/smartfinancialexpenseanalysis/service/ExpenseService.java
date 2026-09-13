package com.smartfinancialexpenseanalysis.service;

import com.smartfinancialexpenseanalysis.dto.ApiResponse;
import com.smartfinancialexpenseanalysis.dto.CategoryResponse;
import com.smartfinancialexpenseanalysis.dto.ExpenseRequest;
import com.smartfinancialexpenseanalysis.dto.ExpenseResponse;
import com.smartfinancialexpenseanalysis.entity.Category;
import com.smartfinancialexpenseanalysis.entity.Expense;
import com.smartfinancialexpenseanalysis.entity.PaymentMethod;
import com.smartfinancialexpenseanalysis.entity.User;
import com.smartfinancialexpenseanalysis.exception.BadRequestException;
import com.smartfinancialexpenseanalysis.exception.ResourceNotFoundException;
import com.smartfinancialexpenseanalysis.exception.UnauthorizedException;
import com.smartfinancialexpenseanalysis.repository.CategoryRepository;
import com.smartfinancialexpenseanalysis.entity.PaymentOption;
import com.smartfinancialexpenseanalysis.repository.ExpenseRepository;
import com.smartfinancialexpenseanalysis.repository.ExpenseSpecification;
import com.smartfinancialexpenseanalysis.repository.PaymentOptionRepository;
import com.smartfinancialexpenseanalysis.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service managing financial expenses with strict user ownership enforcement.
 */
@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final PaymentOptionRepository paymentOptionRepository;

    public ExpenseService(ExpenseRepository expenseRepository,
                          CategoryRepository categoryRepository,
                          UserRepository userRepository,
                          PaymentOptionRepository paymentOptionRepository) {
        this.expenseRepository = expenseRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.paymentOptionRepository = paymentOptionRepository;
    }

    /**
     * Creates a new expense record for the currently authenticated user.
     *
     * @param request   validated expense creation details
     * @param userEmail email of the authenticated user
     * @return safe ExpenseResponse DTO
     */
    @Transactional
    public ExpenseResponse createExpense(ExpenseRequest request, String userEmail) {
        User user = getUserByEmail(userEmail);

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Amount must be greater than zero");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        validatePaymentOption(request.getPaymentMethod());

        String trimmedDescription = request.getDescription() != null ? request.getDescription().trim() : null;

        Expense expense = new Expense(
                user,
                category,
                request.getAmount(),
                request.getDate(),
                request.getPaymentMethod(),
                trimmedDescription
        );

        Expense savedExpense = expenseRepository.save(expense);
        return mapToResponse(savedExpense);
    }

    /**
     * Retrieves expenses belonging exclusively to the authenticated user,
     * applying any combination of search and filter parameters.
     *
     * @param userEmail     email of authenticated user
     * @param search        optional search query matching description
     * @param categoryId    optional category filter
     * @param paymentMethod optional payment method filter
     * @param startDate     optional start date (inclusive)
     * @param endDate       optional end date (inclusive)
     * @param page          optional page index (0-based)
     * @param size          optional page size
     * @return list of matching ExpenseResponse DTOs
     */
    @Transactional(readOnly = true)
    public List<ExpenseResponse> getExpenses(String userEmail,
                                            String search,
                                            Long categoryId,
                                            PaymentMethod paymentMethod,
                                            LocalDate startDate,
                                            LocalDate endDate,
                                            Integer page,
                                            Integer size) {
        return getExpenses(userEmail, search, categoryId, paymentMethod, startDate, endDate, null, null, page, size);
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getExpenses(String userEmail,
                                            String search,
                                            Long categoryId,
                                            PaymentMethod paymentMethod,
                                            LocalDate startDate,
                                            LocalDate endDate,
                                            BigDecimal minAmount,
                                            BigDecimal maxAmount,
                                            Integer page,
                                            Integer size) {
        User user = getUserByEmail(userEmail);

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestException("Start date cannot be after end date");
        }

        if (minAmount != null && maxAmount != null && minAmount.compareTo(maxAmount) > 0) {
            throw new BadRequestException("Minimum amount cannot be greater than maximum amount");
        }

        if (minAmount != null && minAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Minimum amount cannot be negative");
        }

        if (maxAmount != null && maxAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Maximum amount cannot be negative");
        }

        Specification<Expense> spec = ExpenseSpecification.filter(
                user.getId(), search, categoryId, paymentMethod, startDate, endDate, minAmount, maxAmount
        );

        Sort sort = Sort.by(Sort.Order.desc("date"), Sort.Order.desc("id"));

        if (page != null && size != null) {
            int sanitizedPage = Math.max(0, page);
            int sanitizedSize = Math.min(Math.max(1, size), 100);
            Page<Expense> expensePage = expenseRepository.findAll(spec, PageRequest.of(sanitizedPage, sanitizedSize, sort));
            return expensePage.stream().map(this::mapToResponse).collect(Collectors.toList());
        }

        List<Expense> expenses = expenseRepository.findAll(spec, sort);
        return expenses.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    /**
     * Retrieves a single expense by ID, strictly verifying that it belongs to the authenticated user.
     *
     * @param id        expense ID
     * @param userEmail email of authenticated user
     * @return ExpenseResponse DTO
     */
    @Transactional(readOnly = true)
    public ExpenseResponse getExpenseById(Long id, String userEmail) {
        User user = getUserByEmail(userEmail);

        Expense expense = expenseRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));

        return mapToResponse(expense);
    }

    /**
     * Updates an existing expense belonging to the authenticated user.
     *
     * @param id        ID of expense to update
     * @param request   updated expense fields
     * @param userEmail email of authenticated user
     * @return updated ExpenseResponse DTO
     */
    @Transactional
    public ExpenseResponse updateExpense(Long id, ExpenseRequest request, String userEmail) {
        User user = getUserByEmail(userEmail);

        Expense expense = expenseRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Amount must be greater than zero");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        validatePaymentOption(request.getPaymentMethod());

        expense.setAmount(request.getAmount());
        expense.setCategory(category);
        expense.setDate(request.getDate());
        expense.setPaymentMethod(request.getPaymentMethod());
        expense.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);

        Expense updatedExpense = expenseRepository.save(expense);
        return mapToResponse(updatedExpense);
    }

    private void validatePaymentOption(PaymentMethod paymentMethod) {
        if (paymentMethod == null || paymentMethod.getValue() == null || paymentMethod.getValue().trim().isEmpty()) {
            throw new BadRequestException("Payment method is required");
        }
        String val = paymentMethod.getValue().trim();
        Optional<PaymentOption> optionOpt = paymentOptionRepository.findByNameIgnoreCase(val);
        if (optionOpt.isPresent()) {
            if (!optionOpt.get().isActive()) {
                throw new BadRequestException("Payment option '" + val + "' is inactive");
            }
        } else {
            boolean isStandard = "CASH".equalsIgnoreCase(val) || "UPI".equalsIgnoreCase(val) ||
                    "CARD".equalsIgnoreCase(val) || "BANK_TRANSFER".equalsIgnoreCase(val) ||
                    "OTHER".equalsIgnoreCase(val);
            if (!isStandard) {
                throw new BadRequestException("Invalid payment option: " + val);
            }
        }
    }

    /**
     * Deletes an expense belonging to the authenticated user.
     *
     * @param id        ID of expense to delete
     * @param userEmail email of authenticated user
     * @return confirmation ApiResponse
     */
    @Transactional
    public ApiResponse deleteExpense(Long id, String userEmail) {
        User user = getUserByEmail(userEmail);

        Expense expense = expenseRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));

        expenseRepository.delete(expense);
        return new ApiResponse(true, "Expense deleted successfully");
    }

    private User getUserByEmail(String email) {
        if (email == null || email.trim().isEmpty() || "anonymousUser".equalsIgnoreCase(email)) {
            throw new UnauthorizedException("User is not authenticated");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
    }

    private ExpenseResponse mapToResponse(Expense expense) {
        Category category = expense.getCategory();
        CategoryResponse categoryResponse = new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription()
        );

        return new ExpenseResponse(
                expense.getId(),
                expense.getAmount(),
                categoryResponse,
                expense.getDate(),
                expense.getPaymentMethod(),
                expense.getDescription()
        );
    }
}
