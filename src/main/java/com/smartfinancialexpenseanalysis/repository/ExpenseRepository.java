package com.smartfinancialexpenseanalysis.repository;

import com.smartfinancialexpenseanalysis.entity.Expense;
import com.smartfinancialexpenseanalysis.entity.PaymentMethod;
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

    /**
     * Calculate total expenses for an authenticated user across all time.
     * Returns BigDecimal.ZERO if no expenses exist.
     *
     * @param userId ID of the user
     * @return Sum of all user expenses, or 0 if none
     */
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.user.id = :userId")
    BigDecimal sumAmountByUserId(@Param("userId") Long userId);

    /**
     * Count total expenses belonging to a specific user.
     *
     * @param userId ID of the user
     * @return total count of expenses
     */
    long countByUserId(Long userId);

    /**
     * Count expenses for a user within a specified date range.
     *
     * @param userId    ID of the user
     * @param startDate start date (inclusive)
     * @param endDate   end date (inclusive)
     * @return count of matching expenses
     */
    long countByUserIdAndDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * Find the maximum individual expense amount belonging to a specific user.
     * Returns BigDecimal.ZERO if no expenses exist.
     *
     * @param userId ID of the user
     * @return Highest expense amount, or 0 if none
     */
    @Query("SELECT COALESCE(MAX(e.amount), 0) FROM Expense e WHERE e.user.id = :userId")
    BigDecimal findMaxAmountByUserId(@Param("userId") Long userId);

    /**
     * Group expenses by category for a user, returning category ID, category Name, and sum of amounts.
     * Sorted descending by sum of amounts.
     *
     * @param userId ID of the user
     * @return List of Object arrays [Long categoryId, String categoryName, BigDecimal totalAmount]
     */
    @Query("SELECT e.category.id, e.category.name, SUM(e.amount) FROM Expense e WHERE e.user.id = :userId GROUP BY e.category.id, e.category.name ORDER BY SUM(e.amount) DESC")
    List<Object[]> sumAmountByUserIdGroupByCategory(@Param("userId") Long userId);

    /**
     * Group expenses by payment method for a user, returning payment method and sum of amounts.
     * Sorted descending by sum of amounts.
     *
     * @param userId ID of the user
     * @return List of Object arrays [PaymentMethod paymentMethod, BigDecimal totalAmount]
     */
    @Query("SELECT e.paymentMethod, SUM(e.amount) FROM Expense e WHERE e.user.id = :userId GROUP BY e.paymentMethod ORDER BY SUM(e.amount) DESC")
    List<Object[]> sumAmountByUserIdGroupByPaymentMethod(@Param("userId") Long userId);

    /**
     * Find the 5 most recent expenses for a user, ordered by date descending and id descending.
     *
     * @param userId ID of the user
     * @return List of up to 5 recent expenses
     */
    List<Expense> findTop5ByUserIdOrderByDateDescIdDesc(Long userId);

    /**
     * Find the minimum individual expense amount for a user within a specified date range.
     * Returns BigDecimal.ZERO if no expenses exist.
     *
     * @param userId    ID of the user
     * @param startDate start date (inclusive)
     * @param endDate   end date (inclusive)
     * @return Minimum expense amount, or 0 if none
     */
    @Query("SELECT COALESCE(MIN(e.amount), 0) FROM Expense e WHERE e.user.id = :userId AND e.date >= :startDate AND e.date <= :endDate")
    BigDecimal findMinAmountByUserIdAndDateBetween(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find the maximum individual expense amount for a user within a specified date range.
     * Returns BigDecimal.ZERO if no expenses exist.
     *
     * @param userId    ID of the user
     * @param startDate start date (inclusive)
     * @param endDate   end date (inclusive)
     * @return Maximum expense amount, or 0 if none
     */
    @Query("SELECT COALESCE(MAX(e.amount), 0) FROM Expense e WHERE e.user.id = :userId AND e.date >= :startDate AND e.date <= :endDate")
    BigDecimal findMaxAmountByUserIdAndDateBetween(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Group expenses by category for a user within a date range, returning category ID, name, sum of amounts, and count.
     * Sorted descending by sum of amounts.
     *
     * @param userId    ID of the user
     * @param startDate start date (inclusive)
     * @param endDate   end date (inclusive)
     * @return List of Object arrays [Long categoryId, String categoryName, BigDecimal totalAmount, Long count]
     */
    @Query("SELECT e.category.id, e.category.name, SUM(e.amount), COUNT(e.id) FROM Expense e WHERE e.user.id = :userId AND e.date >= :startDate AND e.date <= :endDate GROUP BY e.category.id, e.category.name ORDER BY SUM(e.amount) DESC")
    List<Object[]> sumAmountAndCountByUserIdAndDateBetweenGroupByCategory(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Group expenses by payment method for a user within a date range, returning payment method, sum of amounts, and count.
     * Sorted descending by sum of amounts.
     *
     * @param userId    ID of the user
     * @param startDate start date (inclusive)
     * @param endDate   end date (inclusive)
     * @return List of Object arrays [PaymentMethod paymentMethod, BigDecimal totalAmount, Long count]
     */
    @Query("SELECT e.paymentMethod, SUM(e.amount), COUNT(e.id) FROM Expense e WHERE e.user.id = :userId AND e.date >= :startDate AND e.date <= :endDate GROUP BY e.paymentMethod ORDER BY SUM(e.amount) DESC")
    List<Object[]> sumAmountAndCountByUserIdAndDateBetweenGroupByPaymentMethod(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Find all expenses for a user within a date range ordered by date descending and id descending.
     *
     * @param userId    ID of the user
     * @param startDate start date (inclusive)
     * @param endDate   end date (inclusive)
     * @return List of matching expenses ordered with newest first
     */
    List<Expense> findByUserIdAndDateBetweenOrderByDateDescIdDesc(Long userId, LocalDate startDate, LocalDate endDate);

    /**
     * Check if any expenses reference a specific category.
     */
    boolean existsByCategoryId(Long categoryId);

    /**
     * Count expenses referencing a specific category.
     */
    long countByCategoryId(Long categoryId);

    /**
     * Calculate total system-wide expense amount.
     */
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e")
    BigDecimal sumAllAmount();

    /**
     * Find highest individual expense amount system-wide.
     */
    @Query("SELECT COALESCE(MAX(e.amount), 0) FROM Expense e")
    BigDecimal findMaxAmountSystemWide();

    /**
     * Calculate system-wide expense amount within a date range.
     */
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.date >= :startDate AND e.date <= :endDate")
    BigDecimal sumAmountByDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * System-wide category breakdown: [Long categoryId, String categoryName, BigDecimal totalAmount, Long count].
     */
    @Query("SELECT e.category.id, e.category.name, SUM(e.amount), COUNT(e.id) FROM Expense e GROUP BY e.category.id, e.category.name ORDER BY SUM(e.amount) DESC")
    List<Object[]> sumAmountAndCountGroupByCategory();

    /**
     * System-wide payment method breakdown: [PaymentMethod paymentMethod, BigDecimal totalAmount, Long count].
     */
    @Query("SELECT e.paymentMethod, SUM(e.amount), COUNT(e.id) FROM Expense e GROUP BY e.paymentMethod ORDER BY SUM(e.amount) DESC")
    List<Object[]> sumAmountAndCountGroupByPaymentMethod();

    /**
     * System-wide monthly expenses from a given start date: [Integer year, Integer month, BigDecimal totalAmount, Long count].
     */
    @Query("SELECT YEAR(e.date), MONTH(e.date), SUM(e.amount), COUNT(e.id) FROM Expense e WHERE e.date >= :startDate GROUP BY YEAR(e.date), MONTH(e.date) ORDER BY YEAR(e.date) ASC, MONTH(e.date) ASC")
    List<Object[]> sumAmountAndCountByMonthSince(@Param("startDate") LocalDate startDate);

    /**
     * Find earliest expense date for a user.
     */
    @Query("SELECT MIN(e.date) FROM Expense e WHERE e.user.id = :userId")
    LocalDate findEarliestDateByUserId(@Param("userId") Long userId);

    /**
     * Check if any expenses reference a specific payment method.
     */
    boolean existsByPaymentMethod(PaymentMethod paymentMethod);
}
