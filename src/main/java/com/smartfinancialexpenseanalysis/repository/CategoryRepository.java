package com.smartfinancialexpenseanalysis.repository;

import com.smartfinancialexpenseanalysis.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for Category entities.
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Find a category by its unique name.
     *
     * @param name category name
     * @return Optional containing the found category, or empty if not found
     */
    Optional<Category> findByName(String name);

    Optional<Category> findByNameIgnoreCase(String name);

    /**
     * Check if a category with the given name exists.
     *
     * @param name category name
     * @return true if category exists, false otherwise
     */
    boolean existsByName(String name);

    /**
     * Check if a category with the given name exists ignoring case.
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Check if another category exists with the given name ignoring case.
     */
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
