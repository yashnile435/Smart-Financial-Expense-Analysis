package com.smartfinancialexpenseanalysis.repository;

import com.smartfinancialexpenseanalysis.entity.RecurringExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for RecurringExpense entity.
 */
@Repository
public interface RecurringExpenseRepository extends JpaRepository<RecurringExpense, Long> {

    List<RecurringExpense> findByUserIdOrderByNextOccurrenceAsc(Long userId);

    Optional<RecurringExpense> findByIdAndUserId(Long id, Long userId);

    List<RecurringExpense> findByUserIdAndActiveTrueAndNextOccurrenceLessThanEqual(Long userId, LocalDate date);

    List<RecurringExpense> findByActiveTrueAndNextOccurrenceLessThanEqual(LocalDate date);

    long countByUserIdAndActiveTrue(Long userId);
}
