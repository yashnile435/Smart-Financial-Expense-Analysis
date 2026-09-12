package com.smartfinancialexpenseanalysis.repository;

import com.smartfinancialexpenseanalysis.entity.Budget;
import com.smartfinancialexpenseanalysis.entity.Category;
import com.smartfinancialexpenseanalysis.entity.Expense;
import com.smartfinancialexpenseanalysis.entity.PaymentMethod;
import com.smartfinancialexpenseanalysis.entity.Role;
import com.smartfinancialexpenseanalysis.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for Stage 2 entities and Spring Data JPA repositories.
 * 
 * Uses the configured database to verify CRUD operations,
 * unique constraints, custom repository queries, and foreign key mappings.
 */
@SpringBootTest
@Transactional
class EntityRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    private User testUser;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        // Create base entities for testing
        testUser = new User("Alice Johnson", "alice." + System.currentTimeMillis() + "@example.com", "plainPassword123", Role.USER);
        testUser = userRepository.save(testUser);

        testCategory = new Category("Groceries-" + System.currentTimeMillis(), "Daily grocery and household items");
        testCategory = categoryRepository.save(testCategory);
    }

    @Test
    @DisplayName("User CRUD and unique email constraint")
    void testUserCrudAndEmailConstraint() {
        Optional<User> found = userRepository.findByEmail(testUser.getEmail());
        assertTrue(found.isPresent());
        assertEquals("Alice Johnson", found.get().getName());
        assertEquals(Role.USER, found.get().getRole());

        assertTrue(userRepository.existsByEmail(testUser.getEmail()));
        assertFalse(userRepository.existsByEmail("nonexistent@example.com"));

        // Test unique email constraint
        User duplicateUser = new User("Another Alice", testUser.getEmail(), "secret456", Role.USER);
        assertThrows(DataIntegrityViolationException.class, () -> {
            userRepository.saveAndFlush(duplicateUser);
        });
    }

    @Test
    @DisplayName("Category CRUD and unique name constraint")
    void testCategoryCrudAndNameConstraint() {
        Optional<Category> found = categoryRepository.findByName(testCategory.getName());
        assertTrue(found.isPresent());
        assertEquals(testCategory.getDescription(), found.get().getDescription());

        assertTrue(categoryRepository.existsByName(testCategory.getName()));
        assertFalse(categoryRepository.existsByName("Random Nonexistent Category"));

        // Test unique category name constraint
        Category duplicateCategory = new Category(testCategory.getName(), "Duplicate description");
        assertThrows(DataIntegrityViolationException.class, () -> {
            categoryRepository.saveAndFlush(duplicateCategory);
        });
    }

    @Test
    @DisplayName("Expense creation, relationship mapping, and queries")
    void testExpenseCreationAndQueries() {
        LocalDate today = LocalDate.now();
        Expense expense1 = new Expense(
                testUser,
                testCategory,
                new BigDecimal("150.50"),
                today,
                PaymentMethod.UPI,
                "Supermarket purchase"
        );
        Expense expense2 = new Expense(
                testUser,
                testCategory,
                new BigDecimal("45.00"),
                today.minusDays(2),
                PaymentMethod.CASH,
                "Convenience store snack"
        );

        expenseRepository.save(expense1);
        expenseRepository.save(expense2);

        List<Expense> userExpenses = expenseRepository.findByUserId(testUser.getId());
        assertEquals(2, userExpenses.size());

        List<Expense> dateFiltered = expenseRepository.findByUserIdAndDateBetween(
                testUser.getId(),
                today.minusDays(1),
                today.plusDays(1)
        );
        assertEquals(1, dateFiltered.size());
        assertEquals(new BigDecimal("150.50"), dateFiltered.get(0).getAmount());
        assertEquals(PaymentMethod.UPI, dateFiltered.get(0).getPaymentMethod());

        List<Expense> categoryExpenses = expenseRepository.findByCategoryId(testCategory.getId());
        assertEquals(2, categoryExpenses.size());
    }

    @Test
    @DisplayName("Budget creation and unique constraint on user + month + year")
    void testBudgetCreationAndUniqueConstraint() {
        Budget budget = new Budget(testUser, 10, 2026, new BigDecimal("25000.00"));
        Budget savedBudget = budgetRepository.save(budget);

        assertNotNull(savedBudget.getId());
        assertEquals(10, savedBudget.getMonth());
        assertEquals(2026, savedBudget.getYear());
        assertEquals(new BigDecimal("25000.00"), savedBudget.getAmount());

        Optional<Budget> foundBudget = budgetRepository.findByUserIdAndMonthAndYear(testUser.getId(), 10, 2026);
        assertTrue(foundBudget.isPresent());
        assertEquals(new BigDecimal("25000.00"), foundBudget.get().getAmount());

        assertTrue(budgetRepository.existsByUserIdAndMonthAndYear(testUser.getId(), 10, 2026));
        assertFalse(budgetRepository.existsByUserIdAndMonthAndYear(testUser.getId(), 11, 2026));

        // Test duplicate budget for same user, month, and year violates uniqueness
        Budget duplicateBudget = new Budget(testUser, 10, 2026, new BigDecimal("30000.00"));
        assertThrows(DataIntegrityViolationException.class, () -> {
            budgetRepository.saveAndFlush(duplicateBudget);
        });
    }
}
