package com.smartfinancialexpenseanalysis.service;

import com.smartfinancialexpenseanalysis.dto.ApiResponse;
import com.smartfinancialexpenseanalysis.dto.RecurringExpenseRequest;
import com.smartfinancialexpenseanalysis.dto.RecurringExpenseResponse;
import com.smartfinancialexpenseanalysis.entity.Category;
import com.smartfinancialexpenseanalysis.entity.Expense;
import com.smartfinancialexpenseanalysis.entity.RecurringExpense;
import com.smartfinancialexpenseanalysis.entity.User;
import com.smartfinancialexpenseanalysis.exception.BadRequestException;
import com.smartfinancialexpenseanalysis.exception.ResourceNotFoundException;
import com.smartfinancialexpenseanalysis.exception.UnauthorizedException;
import com.smartfinancialexpenseanalysis.repository.CategoryRepository;
import com.smartfinancialexpenseanalysis.repository.ExpenseRepository;
import com.smartfinancialexpenseanalysis.repository.RecurringExpenseRepository;
import com.smartfinancialexpenseanalysis.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service managing recurring expense configurations and automated due occurrence generation.
 */
@Service
public class RecurringExpenseService {

    private final RecurringExpenseRepository recurringExpenseRepository;
    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public RecurringExpenseService(RecurringExpenseRepository recurringExpenseRepository,
                                   ExpenseRepository expenseRepository,
                                   CategoryRepository categoryRepository,
                                   UserRepository userRepository) {
        this.recurringExpenseRepository = recurringExpenseRepository;
        this.expenseRepository = expenseRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<RecurringExpenseResponse> getRecurringExpenses(String userEmail) {
        User user = getUserByEmail(userEmail);
        return recurringExpenseRepository.findByUserIdOrderByNextOccurrenceAsc(user.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RecurringExpenseResponse getRecurringExpenseById(Long id, String userEmail) {
        User user = getUserByEmail(userEmail);
        RecurringExpense entity = recurringExpenseRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Recurring expense not found with ID: " + id));
        return mapToResponse(entity);
    }

    @Transactional
    public RecurringExpenseResponse createRecurringExpense(RecurringExpenseRequest request, String userEmail) {
        User user = getUserByEmail(userEmail);

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Amount must be greater than zero");
        }

        if (request.getStartDate() == null) {
            throw new BadRequestException("Start date is required");
        }

        if (request.getEndDate() != null && request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date cannot be before start date");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        String desc = request.getDescription() != null ? request.getDescription().trim() : null;

        RecurringExpense recurring = new RecurringExpense(
                user,
                category,
                request.getAmount(),
                desc,
                request.getPaymentMethod(),
                request.getFrequency(),
                request.getStartDate(),
                request.getEndDate(),
                request.getStartDate(),
                true
        );

        RecurringExpense saved = recurringExpenseRepository.save(recurring);
        return mapToResponse(saved);
    }

    @Transactional
    public RecurringExpenseResponse updateRecurringExpense(Long id, RecurringExpenseRequest request, String userEmail) {
        User user = getUserByEmail(userEmail);
        RecurringExpense existing = recurringExpenseRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Recurring expense not found with ID: " + id));

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Amount must be greater than zero");
        }

        if (request.getStartDate() == null) {
            throw new BadRequestException("Start date is required");
        }

        if (request.getEndDate() != null && request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date cannot be before start date");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        existing.setCategory(category);
        existing.setAmount(request.getAmount());
        existing.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        existing.setPaymentMethod(request.getPaymentMethod());
        existing.setFrequency(request.getFrequency());
        existing.setStartDate(request.getStartDate());
        existing.setEndDate(request.getEndDate());

        // If next occurrence is before the new start date, advance it
        if (existing.getNextOccurrence().isBefore(request.getStartDate())) {
            existing.setNextOccurrence(request.getStartDate());
        }

        RecurringExpense updated = recurringExpenseRepository.save(existing);
        return mapToResponse(updated);
    }

    @Transactional
    public RecurringExpenseResponse toggleStatus(Long id, String userEmail) {
        User user = getUserByEmail(userEmail);
        RecurringExpense existing = recurringExpenseRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Recurring expense not found with ID: " + id));

        existing.setActive(!existing.isActive());
        RecurringExpense saved = recurringExpenseRepository.save(existing);
        return mapToResponse(saved);
    }

    @Transactional
    public ApiResponse deleteRecurringExpense(Long id, String userEmail) {
        User user = getUserByEmail(userEmail);
        RecurringExpense existing = recurringExpenseRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Recurring expense not found with ID: " + id));

        recurringExpenseRepository.delete(existing);
        return new ApiResponse(true, "Recurring expense deleted successfully");
    }

    @Transactional
    public RecurringExpenseResponse generateDueExpense(Long id, String userEmail) {
        User user = getUserByEmail(userEmail);
        RecurringExpense existing = recurringExpenseRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Recurring expense not found with ID: " + id));

        if (!existing.isActive()) {
            throw new BadRequestException("Recurring expense is not active");
        }

        LocalDate today = LocalDate.now();
        if (existing.getNextOccurrence().isAfter(today)) {
            throw new BadRequestException("Recurring expense is not due yet. Next occurrence is " + existing.getNextOccurrence());
        }

        if (existing.getLastGeneratedDate() != null && existing.getLastGeneratedDate().equals(existing.getNextOccurrence())) {
            throw new BadRequestException("Expense for this occurrence has already been generated");
        }

        if (existing.getEndDate() != null && existing.getNextOccurrence().isAfter(existing.getEndDate())) {
            existing.setActive(false);
            recurringExpenseRepository.save(existing);
            throw new BadRequestException("Recurring expense has passed its end date");
        }

        LocalDate occurrenceDate = existing.getNextOccurrence();

        // Create the actual Expense record
        Expense expense = new Expense(
                user,
                existing.getCategory(),
                existing.getAmount(),
                occurrenceDate,
                existing.getPaymentMethod(),
                existing.getDescription() != null ? existing.getDescription() : "Recurring: " + existing.getCategory().getName()
        );
        expenseRepository.save(expense);

        // Update recurrence state
        existing.setLastGeneratedDate(occurrenceDate);
        LocalDate nextDate = existing.getFrequency().nextDate(occurrenceDate);
        existing.setNextOccurrence(nextDate);

        if (existing.getEndDate() != null && nextDate.isAfter(existing.getEndDate())) {
            existing.setActive(false);
        }

        RecurringExpense saved = recurringExpenseRepository.save(existing);
        return mapToResponse(saved);
    }

    @Transactional
    public List<RecurringExpenseResponse> processAllDueExpenses(String userEmail) {
        User user = getUserByEmail(userEmail);
        LocalDate today = LocalDate.now();

        List<RecurringExpense> dueList = recurringExpenseRepository
                .findByUserIdAndActiveTrueAndNextOccurrenceLessThanEqual(user.getId(), today);

        List<RecurringExpenseResponse> results = new ArrayList<>();
        for (RecurringExpense item : dueList) {
            // Guard against duplicate generation on same date
            if (item.getLastGeneratedDate() != null && item.getLastGeneratedDate().equals(item.getNextOccurrence())) {
                continue;
            }

            LocalDate occurrenceDate = item.getNextOccurrence();
            Expense expense = new Expense(
                    user,
                    item.getCategory(),
                    item.getAmount(),
                    occurrenceDate,
                    item.getPaymentMethod(),
                    item.getDescription() != null ? item.getDescription() : "Recurring: " + item.getCategory().getName()
            );
            expenseRepository.save(expense);

            item.setLastGeneratedDate(occurrenceDate);
            LocalDate nextDate = item.getFrequency().nextDate(occurrenceDate);
            item.setNextOccurrence(nextDate);

            if (item.getEndDate() != null && nextDate.isAfter(item.getEndDate())) {
                item.setActive(false);
            }

            RecurringExpense saved = recurringExpenseRepository.save(item);
            results.add(mapToResponse(saved));
        }

        return results;
    }

    /**
     * Portable Spring-scheduled periodic task that runs every hour to process
     * any due recurring expenses across active configurations.
     * Duplicate-safe, idempotent, and portable across all operating systems.
     */
    @Scheduled(cron = "${app.scheduling.recurring-cron:0 0 * * * *}")
    @Transactional
    public void scheduledProcessAllDueExpenses() {
        LocalDate today = LocalDate.now();
        List<RecurringExpense> dueList = recurringExpenseRepository
                .findByActiveTrueAndNextOccurrenceLessThanEqual(today);

        for (RecurringExpense item : dueList) {
            if (!item.isActive()) {
                continue;
            }
            if (item.getLastGeneratedDate() != null && item.getLastGeneratedDate().equals(item.getNextOccurrence())) {
                continue;
            }

            LocalDate occurrenceDate = item.getNextOccurrence();
            Expense expense = new Expense(
                    item.getUser(),
                    item.getCategory(),
                    item.getAmount(),
                    occurrenceDate,
                    item.getPaymentMethod(),
                    item.getDescription() != null ? item.getDescription() : "Recurring: " + item.getCategory().getName()
            );
            expenseRepository.save(expense);

            item.setLastGeneratedDate(occurrenceDate);
            LocalDate nextDate = item.getFrequency().nextDate(occurrenceDate);
            item.setNextOccurrence(nextDate);

            if (item.getEndDate() != null && nextDate.isAfter(item.getEndDate())) {
                item.setActive(false);
            }

            recurringExpenseRepository.save(item);
        }
    }

    private RecurringExpenseResponse mapToResponse(RecurringExpense entity) {
        return new RecurringExpenseResponse(
                entity.getId(),
                entity.getCategory().getId(),
                entity.getCategory().getName(),
                entity.getAmount().setScale(2, RoundingMode.HALF_UP),
                entity.getDescription(),
                entity.getPaymentMethod(),
                entity.getFrequency(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getNextOccurrence(),
                entity.getLastGeneratedDate(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
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
