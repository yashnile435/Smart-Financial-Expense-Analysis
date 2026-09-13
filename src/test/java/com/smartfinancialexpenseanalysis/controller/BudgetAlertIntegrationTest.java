package com.smartfinancialexpenseanalysis.controller;

import com.smartfinancialexpenseanalysis.entity.Budget;
import com.smartfinancialexpenseanalysis.entity.Category;
import com.smartfinancialexpenseanalysis.entity.Expense;
import com.smartfinancialexpenseanalysis.entity.PaymentMethod;
import com.smartfinancialexpenseanalysis.entity.Role;
import com.smartfinancialexpenseanalysis.entity.User;
import com.smartfinancialexpenseanalysis.repository.BudgetRepository;
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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BudgetAlertIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    private User userA;
    private User userB;
    private Category category;

    @BeforeEach
    void setUp() {
        budgetRepository.deleteAll();
        expenseRepository.deleteAll();

        userA = userRepository.findByEmail("userA@example.com")
                .orElseGet(() -> userRepository.save(new User("User A", "userA@example.com", "Password@123", Role.USER, true)));

        userB = userRepository.findByEmail("userB@example.com")
                .orElseGet(() -> userRepository.save(new User("User B", "userB@example.com", "Password@123", Role.USER, true)));

        category = categoryRepository.findByNameIgnoreCase("Shopping")
                .orElseGet(() -> categoryRepository.save(new Category("Shopping", "Shopping expenses")));
    }

    @Test
    @DisplayName("Unauthenticated request to budget alerts returns 401")
    void unauthenticatedAlertsReturns401() throws Exception {
        mockMvc.perform(get("/api/budgets/alerts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Budget utilization under 80% returns NORMAL alert level")
    @WithMockUser(username = "userA@example.com")
    void budgetUnder80ReturnsNormal() throws Exception {
        int month = 4;
        int year = 2026;
        Budget budget = new Budget(userA, month, year, new BigDecimal("10000.00"));
        budgetRepository.save(budget);

        LocalDate date = YearMonth.of(year, month).atDay(10);
        expenseRepository.save(new Expense(userA, category, new BigDecimal("5000.00"), date, PaymentMethod.UPI, "Clothes"));

        mockMvc.perform(get("/api/budgets/alerts?month=" + month + "&year=" + year))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].alertLevel").value("NORMAL"))
                .andExpect(jsonPath("$[0].utilizationPercentage").value(50.0));
    }

    @Test
    @DisplayName("Budget utilization 80-89% returns WARNING alert level")
    @WithMockUser(username = "userA@example.com")
    void budgetBetween80And89ReturnsWarning() throws Exception {
        int month = 5;
        int year = 2026;
        Budget budget = new Budget(userA, month, year, new BigDecimal("10000.00"));
        budgetRepository.save(budget);

        LocalDate date = YearMonth.of(year, month).atDay(10);
        expenseRepository.save(new Expense(userA, category, new BigDecimal("8500.00"), date, PaymentMethod.UPI, "Clothes"));

        mockMvc.perform(get("/api/budgets/alerts?month=" + month + "&year=" + year))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].alertLevel").value("WARNING"))
                .andExpect(jsonPath("$[0].utilizationPercentage").value(85.0));
    }

    @Test
    @DisplayName("Budget utilization 90-99% returns CRITICAL_WARNING alert level")
    @WithMockUser(username = "userA@example.com")
    void budgetBetween90And99ReturnsCriticalWarning() throws Exception {
        int month = 6;
        int year = 2026;
        Budget budget = new Budget(userA, month, year, new BigDecimal("10000.00"));
        budgetRepository.save(budget);

        LocalDate date = YearMonth.of(year, month).atDay(10);
        expenseRepository.save(new Expense(userA, category, new BigDecimal("9500.00"), date, PaymentMethod.UPI, "Clothes"));

        mockMvc.perform(get("/api/budgets/alerts?month=" + month + "&year=" + year))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].alertLevel").value("CRITICAL_WARNING"))
                .andExpect(jsonPath("$[0].utilizationPercentage").value(95.0));
    }

    @Test
    @DisplayName("Budget utilization >= 100% returns EXCEEDED alert level")
    @WithMockUser(username = "userA@example.com")
    void budgetAbove100ReturnsExceeded() throws Exception {
        int month = 7;
        int year = 2026;
        Budget budget = new Budget(userA, month, year, new BigDecimal("10000.00"));
        budgetRepository.save(budget);

        LocalDate date = YearMonth.of(year, month).atDay(10);
        expenseRepository.save(new Expense(userA, category, new BigDecimal("12000.00"), date, PaymentMethod.UPI, "Clothes"));

        mockMvc.perform(get("/api/budgets/alerts?month=" + month + "&year=" + year))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].alertLevel").value("EXCEEDED"))
                .andExpect(jsonPath("$[0].utilizationPercentage").value(120.0));
    }

    @Test
    @DisplayName("Budget alerts respect user data isolation")
    @WithMockUser(username = "userA@example.com")
    void userIsolationEnforced() throws Exception {
        int month = 8;
        int year = 2026;
        Budget budgetB = new Budget(userB, month, year, new BigDecimal("20000.00"));
        budgetRepository.save(budgetB);

        mockMvc.perform(get("/api/budgets/alerts?month=" + month + "&year=" + year))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
