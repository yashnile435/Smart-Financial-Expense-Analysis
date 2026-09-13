package com.smartfinancialexpenseanalysis.repository;

import com.smartfinancialexpenseanalysis.entity.Expense;
import com.smartfinancialexpenseanalysis.entity.PaymentMethod;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Spring Data JPA Specification builder for dynamic expense querying.
 * All queries strictly enforce user ownership.
 */
public class ExpenseSpecification {

    /**
     * Legacy 6-parameter filter method for backward compatibility.
     */
    public static Specification<Expense> filter(Long userId,
                                                String search,
                                                Long categoryId,
                                                PaymentMethod paymentMethod,
                                                LocalDate startDate,
                                                LocalDate endDate) {
        return filter(userId, search, categoryId, paymentMethod, startDate, endDate, null, null);
    }

    /**
     * Builds a specification combining ownership enforcement, search, and all filters including amount ranges.
     *
     * @param userId        ID of the authenticated user (mandatory)
     * @param search        Search term for description (optional)
     * @param categoryId    Category ID filter (optional)
     * @param paymentMethod PaymentMethod filter (optional)
     * @param startDate     Start date inclusive (optional)
     * @param endDate       End date inclusive (optional)
     * @param minAmount     Minimum amount inclusive (optional)
     * @param maxAmount     Maximum amount inclusive (optional)
     * @return Specification representing the combined criteria
     */
    public static Specification<Expense> filter(Long userId,
                                                String search,
                                                Long categoryId,
                                                PaymentMethod paymentMethod,
                                                LocalDate startDate,
                                                LocalDate endDate,
                                                BigDecimal minAmount,
                                                BigDecimal maxAmount) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Mandatory ownership constraint: must belong to the authenticated user
            predicates.add(criteriaBuilder.equal(root.get("user").get("id"), userId));

            // 2. Optional text search on description
            if (search != null && !search.trim().isEmpty()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern));
            }

            // 3. Optional category filter
            if (categoryId != null) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("id"), categoryId));
            }

            // 4. Optional payment method filter
            if (paymentMethod != null) {
                predicates.add(criteriaBuilder.equal(root.get("paymentMethod"), paymentMethod));
            }

            // 5. Optional date range filters
            if (startDate != null && endDate != null) {
                predicates.add(criteriaBuilder.between(root.get("date"), startDate, endDate));
            } else if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("date"), startDate));
            } else if (endDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("date"), endDate));
            }

            // 6. Optional min amount filter
            if (minAmount != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("amount"), minAmount));
            }

            // 7. Optional max amount filter
            if (maxAmount != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("amount"), maxAmount));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
