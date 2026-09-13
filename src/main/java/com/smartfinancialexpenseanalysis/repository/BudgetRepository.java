package com.smartfinancialexpenseanalysis.repository;

import com.smartfinancialexpenseanalysis.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Budget entities.
 */
@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    /**
     * Find a budget for a user for a specific month and year.
     *
     * @param userId ID of the user
     * @param month  month (1 - 12)
     * @param year   year (e.g. 2026)
     * @return Optional containing the budget, or empty if not set
     */
    Optional<Budget> findByUserIdAndMonthAndYear(Long userId, int month, int year);

    /**
     * Find all budgets set by a specific user.
     *
     * @param userId ID of the user
     * @return List of budgets
     */
    List<Budget> findByUserId(Long userId);

    /**
     * Find all budgets for a user for a specific year.
     *
     * @param userId ID of the user
     * @param year   year
     * @return List of budgets
     */
    List<Budget> findByUserIdAndYear(Long userId, int year);

    /**
     * Check if a budget already exists for a user in a given month and year.
     *
     * @param userId ID of the user
     * @param month  month (1 - 12)
     * @param year   year
     * @return true if budget exists, false otherwise
     */
    boolean existsByUserIdAndMonthAndYear(Long userId, int month, int year);

    /**
     * Find a budget by its primary ID and user ID for ownership verification.
     *
     * @param id     ID of the budget
     * @param userId ID of the user
     * @return Optional containing matching budget if owned by user
     */
    Optional<Budget> findByIdAndUserId(Long id, Long userId);

    /**
     * Find all budgets for a user ordered by year descending and month descending.
     *
     * @param userId ID of the user
     * @return List of budgets ordered with most recent first
     */
    List<Budget> findByUserIdOrderByYearDescMonthDesc(Long userId);

    /**
     * Calculate total system-wide budget amount.
     */
    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(b.amount), 0) FROM Budget b")
    java.math.BigDecimal sumAllAmount();
}
