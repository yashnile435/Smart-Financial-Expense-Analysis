package com.smartfinancialexpenseanalysis.repository;

import com.smartfinancialexpenseanalysis.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for User entities.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by their unique email address.
     *
     * @param email user's email address
     * @return Optional containing the found user, or empty if not found
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if a user with the given email already exists.
     *
     * @param email email to check
     * @return true if user exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Count users with enabled status true.
     */
    long countByEnabledTrue();

    /**
     * Count users with a specific role.
     */
    long countByRole(com.smartfinancialexpenseanalysis.entity.Role role);

    /**
     * Count users with a specific role and enabled status true.
     */
    long countByRoleAndEnabledTrue(com.smartfinancialexpenseanalysis.entity.Role role);

    /**
     * Find all users ordered by ID descending.
     */
    java.util.List<User> findAllByOrderByIdDesc();

    /**
     * Search users by name or email (case-insensitive substring match) ordered by ID descending.
     */
    java.util.List<User> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByIdDesc(String name, String email);
}
