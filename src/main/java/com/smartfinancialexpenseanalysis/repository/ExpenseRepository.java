package com.smartfinancialexpenseanalysis.repository;

import com.smartfinancialexpenseanalysis.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Spring Data JPA repository for Expense entities.
 */
@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

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
}
