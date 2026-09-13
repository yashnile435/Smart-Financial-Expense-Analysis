package com.smartfinancialexpenseanalysis.repository;

import com.smartfinancialexpenseanalysis.entity.SavingsGoal;
import com.smartfinancialexpenseanalysis.entity.SavingsGoalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for SavingsGoal entity.
 */
@Repository
public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {

    List<SavingsGoal> findByUserIdOrderByTargetDateAsc(Long userId);

    List<SavingsGoal> findByUserIdAndStatusOrderByTargetDateAsc(Long userId, SavingsGoalStatus status);

    Optional<SavingsGoal> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndStatus(Long userId, SavingsGoalStatus status);
}
