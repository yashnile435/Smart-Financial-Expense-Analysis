package com.smartfinancialexpenseanalysis.controller;

import com.smartfinancialexpenseanalysis.entity.Category;
import com.smartfinancialexpenseanalysis.entity.Expense;
import com.smartfinancialexpenseanalysis.entity.PaymentMethod;
import com.smartfinancialexpenseanalysis.entity.Role;
import com.smartfinancialexpenseanalysis.entity.User;
import com.smartfinancialexpenseanalysis.repository.CategoryRepository;
import com.smartfinancialexpenseanalysis.repository.ExpenseRepository;
import com.smartfinancialexpenseanalysis.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AnalyticsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    private User userA;
    private User userB;
    private Category foodCategory;
    private Category travelCategory;

    @BeforeEach
    void setUp() {
        expenseRepository.deleteAll();

        userA = userRepository.findByEmail("userA@example.com")
                .orElseGet(() -> userRepository.save(new User("User A", "userA@example.com", "Password@123", Role.USER, true)));

        userB = userRepository.findByEmail("userB@example.com")
                .orElseGet(() -> userRepository.save(new User("User B", "userB@example.com", "Password@123", Role.USER, true)));

        foodCategory = categoryRepository.findByNameIgnoreCase("Food")
                .orElseGet(() -> categoryRepository.save(new Category("Food", "Food and Dining")));

        travelCategory = categoryRepository.findByNameIgnoreCase("Travel")
                .orElseGet(() -> categoryRepository.save(new Category("Travel", "Travel expenses")));
    }

    @Test
    @DisplayName("Unauthenticated analytics request returns 401")
    void unauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/analytics/comparison"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Spending comparison shows INCREASED when current spending is higher")
    @WithMockUser(username = "userA@example.com")
    void spendingComparisonIncrease() throws Exception {
        // Month 5 (Current): 25,000
        LocalDate dateCur = YearMonth.of(2026, 5).atDay(10);
        expenseRepository.save(new Expense(userA, foodCategory, new BigDecimal("25000.00"), dateCur, PaymentMethod.UPI, "Groceries"));

        // Month 4 (Comparison): 20,000
        LocalDate dateComp = YearMonth.of(2026, 4).atDay(10);
        expenseRepository.save(new Expense(userA, foodCategory, new BigDecimal("20000.00"), dateComp, PaymentMethod.UPI, "Groceries"));

        mockMvc.perform(get("/api/analytics/comparison?month=5&year=2026&comparisonMonth=4&comparisonYear=2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPeriodTotal").value(25000.0))
                .andExpect(jsonPath("$.comparisonPeriodTotal").value(20000.0))
                .andExpect(jsonPath("$.difference").value(5000.0))
                .andExpect(jsonPath("$.percentageChange").value(25.0))
                .andExpect(jsonPath("$.direction").value("INCREASED"));
    }

    @Test
    @DisplayName("Spending comparison shows DECREASED when current spending is lower")
    @WithMockUser(username = "userA@example.com")
    void spendingComparisonDecrease() throws Exception {
        // Month 6 (Current): 15,000
        LocalDate dateCur = YearMonth.of(2026, 6).atDay(10);
        expenseRepository.save(new Expense(userA, foodCategory, new BigDecimal("15000.00"), dateCur, PaymentMethod.UPI, "Groceries"));

        // Month 5 (Comparison): 20,000
        LocalDate dateComp = YearMonth.of(2026, 5).atDay(10);
        expenseRepository.save(new Expense(userA, foodCategory, new BigDecimal("20000.00"), dateComp, PaymentMethod.UPI, "Groceries"));

        mockMvc.perform(get("/api/analytics/comparison?month=6&year=2026&comparisonMonth=5&comparisonYear=2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPeriodTotal").value(15000.0))
                .andExpect(jsonPath("$.comparisonPeriodTotal").value(20000.0))
                .andExpect(jsonPath("$.difference").value(-5000.0))
                .andExpect(jsonPath("$.percentageChange").value(25.0))
                .andExpect(jsonPath("$.direction").value("DECREASED"));
    }

    @Test
    @DisplayName("Advanced analytics returns highest category, max expense, and correct totals")
    @WithMockUser(username = "userA@example.com")
    void advancedAnalyticsMetrics() throws Exception {
        LocalDate now = LocalDate.now();
        expenseRepository.save(new Expense(userA, foodCategory, new BigDecimal("5000.00"), now, PaymentMethod.UPI, "Dinner"));
        expenseRepository.save(new Expense(userA, travelCategory, new BigDecimal("2000.00"), now, PaymentMethod.CASH, "Cab"));

        mockMvc.perform(get("/api/analytics/advanced"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.highestSpendingCategory").value("Food"))
                .andExpect(jsonPath("$.highestSpendingCategoryAmount").value(5000.0))
                .andExpect(jsonPath("$.lowestSpendingCategory").value("Travel"))
                .andExpect(jsonPath("$.lowestSpendingCategoryAmount").value(2000.0))
                .andExpect(jsonPath("$.largestExpense").value(5000.0))
                .andExpect(jsonPath("$.totalExpenseCount").value(2))
                .andExpect(jsonPath("$.totalExpenseAmount").value(7000.0));
    }
}
