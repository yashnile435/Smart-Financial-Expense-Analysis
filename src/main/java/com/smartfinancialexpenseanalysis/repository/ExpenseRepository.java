package com.smartfinancialexpenseanalysis.repository;

import com.smartfinancialexpenseanalysis.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Expense entities.
 */
@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {

    /**
     * Find a specific expense by its ID and user ID for ownership verification.
     *
     * @param id     ID of the expense
     * @param userId ID of the user
     * @return Optional containing matching expense if owned by user
     */
    Optional<Expense> findByIdAndUserId(Long id, Long userId);

    /**
     * Find all expenses belonging to a specific user.
     *
     * @param userId ID of the user
     * @return List of expenses
     */
    List<Expense> findByUserId(Long userId);

    /**
     * Find all expenses belonging to a specific user ordered by date descending.
     *
     * @param userId ID of the user
     * @return List of expenses ordered with most recent first
     */
    List<Expense> findByUserIdOrderByDateDesc(Long userId);

    /**
     * Find expenses for a user within a specified date range.
     *
     * @param userId    ID of the user
     * @param startDate start date (inclusive)
     * @param endDate   end date (inclusive)
     * @return List of matching expenses
     */
    List<Expense> findByUserIdAndDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * Find all expenses belonging to a specific category.
     *
     * @param categoryId ID of the category
     * @return List of expenses
     */
    List<Expense> findByCategoryId(Long categoryId);

    /**
     * Find all expenses for a specific user within a specific category.
     *
     * @param userId     ID of the user
     * @param categoryId ID of the category
     * @return List of matching expenses
     */
    List<Expense> findByUserIdAndCategoryId(Long userId, Long categoryId);

    /**
     * Calculate the sum of expense amounts for a user within a specified date range.
     * Returns BigDecimal.ZERO (via COALESCE) if no expenses exist in the range.
     *
     * @param userId    ID of the user
     * @param startDate start date (inclusive)
     * @param endDate   end date (inclusive)
     * @return Sum of expenses, or 0 if none
     */
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.user.id = :userId AND e.date >= :startDate AND e.date <= :endDate")
    BigDecimal sumAmountByUserIdAndDateBetween(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
