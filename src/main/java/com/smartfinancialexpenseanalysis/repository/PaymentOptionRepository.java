package com.smartfinancialexpenseanalysis.repository;

import com.smartfinancialexpenseanalysis.entity.PaymentOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for PaymentOption entities.
 */
@Repository
public interface PaymentOptionRepository extends JpaRepository<PaymentOption, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsByNameIgnoreCaseAndActiveTrue(String name);

    Optional<PaymentOption> findByNameIgnoreCase(String name);

    List<PaymentOption> findByActiveTrueOrderByNameAsc();

    List<PaymentOption> findAllByOrderByIdAsc();
}
