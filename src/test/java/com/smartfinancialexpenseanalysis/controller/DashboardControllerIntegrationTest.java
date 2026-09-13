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

/**
 * Comprehensive integration tests for Stage 6 Financial Dashboard & Analytics.
 * Validates aggregations, breakdowns, 6-month trends, rule-based insights, and multi-user isolation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DashboardControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    private User userAlice;
    private User userBob;
    private Category categoryFood;
    private Category categoryTravel;
    private Category categoryShopping;

    @BeforeEach
    void setUp() {
        userAlice = userRepository.findByEmail("alice.dash@example.com").orElseGet(() ->
                userRepository.save(new User("Alice Dash", "alice.dash@example.com", "passwordHash", Role.USER))
        );

        userBob = userRepository.findByEmail("bob.dash@example.com").orElseGet(() ->
                userRepository.save(new User("Bob Dash", "bob.dash@example.com", "passwordHash", Role.USER))
        );

        categoryFood = categoryRepository.findByName("Food").orElseGet(() ->
                categoryRepository.save(new Category("Food", "Food expenses"))
        );

        categoryTravel = categoryRepository.findByName("Travel").orElseGet(() ->
                categoryRepository.save(new Category("Travel", "Travel expenses"))
        );

        categoryShopping = categoryRepository.findByName("Shopping").orElseGet(() ->
                categoryRepository.save(new Category("Shopping", "Shopping expenses"))
        );
    }

    @Test
    @WithMockUser(username = "alice.dash@example.com")
    @DisplayName("1. Authenticated user can retrieve dashboard summary (200 OK)")
    void testGetDashboardSummary_Authenticated_Success() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentMonth").isNumber())
                .andExpect(jsonPath("$.currentYear").isNumber())
                .andExpect(jsonPath("$.totalExpenses").value(0.00))
                .andExpect(jsonPath("$.expenseCount").value(0))
                .andExpect(jsonPath("$.monthlyTrend", hasSize(6)));
    }

    @Test
    @DisplayName("2. Unauthenticated user receives 401 Unauthorized")
    void testGetDashboardSummary_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "alice.dash@example.com")
    @DisplayName("3. Total expenses and count calculations are accurate")
    void testTotalExpensesAndCountCalculations() throws Exception {
        LocalDate today = LocalDate.now();
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("1500.00"), today, PaymentMethod.UPI, "Lunch"));
        expenseRepository.save(new Expense(userAlice, categoryTravel, new BigDecimal("3500.00"), today, PaymentMethod.CARD, "Train ticket"));

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpenses").value(5000.00))
                .andExpect(jsonPath("$.expenseCount").value(2))
                .andExpect(jsonPath("$.averageExpense").value(2500.00))
                .andExpect(jsonPath("$.highestExpense").value(3500.00));
    }

    @Test
    @WithMockUser(username = "alice.dash@example.com")
    @DisplayName("4. Current month expense calculation only includes expenses in current calendar month")
    void testCurrentMonthExpenseCalculation() throws Exception {
        YearMonth now = YearMonth.now();
        LocalDate firstDay = now.atDay(1);
        LocalDate lastDay = now.atEndOfMonth();
        LocalDate prevMonth = now.minusMonths(1).atDay(15);
        LocalDate nextMonth = now.plusMonths(1).atDay(15);

        // Current month expenses (first and last day edge cases)
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("1000.00"), firstDay, PaymentMethod.CASH, "First day"));
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("2000.00"), lastDay, PaymentMethod.UPI, "Last day"));

        // Outside current month expenses
        expenseRepository.save(new Expense(userAlice, categoryTravel, new BigDecimal("5000.00"), prevMonth, PaymentMethod.CARD, "Past month"));
        expenseRepository.save(new Expense(userAlice, categoryTravel, new BigDecimal("4000.00"), nextMonth, PaymentMethod.OTHER, "Future month"));

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpenses").value(12000.00))
                .andExpect(jsonPath("$.expenseCount").value(4))
                .andExpect(jsonPath("$.currentMonthExpenses").value(3000.00))
                .andExpect(jsonPath("$.currentMonthExpenseCount").value(2));
    }

    @Test
    @WithMockUser(username = "alice.dash@example.com")
    @DisplayName("5. Average expense and highest expense calculations")
    void testAverageAndHighestExpense() throws Exception {
        LocalDate today = LocalDate.now();
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("100.00"), today, PaymentMethod.CASH, "Coffee"));
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("200.00"), today, PaymentMethod.UPI, "Snack"));
        expenseRepository.save(new Expense(userAlice, categoryShopping, new BigDecimal("900.00"), today, PaymentMethod.CARD, "Shirt"));

        // Total = 1200, count = 3, avg = 400.00, highest = 900.00
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpenses").value(1200.00))
                .andExpect(jsonPath("$.expenseCount").value(3))
                .andExpect(jsonPath("$.averageExpense").value(400.00))
                .andExpect(jsonPath("$.highestExpense").value(900.00));
    }

    @Test
    @WithMockUser(username = "alice.dash@example.com")
    @DisplayName("6. Current month budget information when budget exists")
    void testCurrentMonthBudget_Exists() throws Exception {
        YearMonth now = YearMonth.now();
        budgetRepository.save(new Budget(userAlice, now.getMonthValue(), now.getYear(), new BigDecimal("20000.00")));
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("14500.00"), now.atDay(10), PaymentMethod.UPI, "Groceries"));

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentMonthBudget").value(20000.00))
                .andExpect(jsonPath("$.currentMonthExpenses").value(14500.00))
                .andExpect(jsonPath("$.currentMonthRemaining").value(5500.00))
                .andExpect(jsonPath("$.currentMonthUtilization").value(72.50))
                .andExpect(jsonPath("$.currentMonthBudgetStatus").value("UNDER_BUDGET"));
    }

    @Test
    @WithMockUser(username = "alice.dash@example.com")
    @DisplayName("7. Current month budget returns safe nulls when no budget configured")
    void testCurrentMonthBudget_NotConfigured() throws Exception {
        YearMonth now = YearMonth.now();
        budgetRepository.findByUserIdAndMonthAndYear(userAlice.getId(), now.getMonthValue(), now.getYear())
                .ifPresent(budgetRepository::delete);

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentMonthBudget").doesNotExist())
                .andExpect(jsonPath("$.currentMonthBudgetStatus").doesNotExist());
    }

    @Test
    @WithMockUser(username = "alice.dash@example.com")
    @DisplayName("8. Category-wise breakdown calculates amounts and percentages correctly")
    void testCategoryBreakdown() throws Exception {
        LocalDate today = LocalDate.now();
        // Food: 5000 (50%), Travel: 3000 (30%), Shopping: 2000 (20%) -> Total 10000
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("5000.00"), today, PaymentMethod.UPI, "Food spend"));
        expenseRepository.save(new Expense(userAlice, categoryTravel, new BigDecimal("3000.00"), today, PaymentMethod.CARD, "Travel spend"));
        expenseRepository.save(new Expense(userAlice, categoryShopping, new BigDecimal("2000.00"), today, PaymentMethod.CASH, "Shopping spend"));

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryBreakdown", hasSize(3)))
                .andExpect(jsonPath("$.categoryBreakdown[0].categoryName").value("Food"))
                .andExpect(jsonPath("$.categoryBreakdown[0].totalAmount").value(5000.00))
                .andExpect(jsonPath("$.categoryBreakdown[0].percentage").value(50.00))
                .andExpect(jsonPath("$.categoryBreakdown[1].categoryName").value("Travel"))
                .andExpect(jsonPath("$.categoryBreakdown[1].totalAmount").value(3000.00))
                .andExpect(jsonPath("$.categoryBreakdown[1].percentage").value(30.00))
                .andExpect(jsonPath("$.categoryBreakdown[2].categoryName").value("Shopping"))
                .andExpect(jsonPath("$.categoryBreakdown[2].totalAmount").value(2000.00))
                .andExpect(jsonPath("$.categoryBreakdown[2].percentage").value(20.00));
    }

    @Test
    @WithMockUser(username = "alice.dash@example.com")
    @DisplayName("9. Payment method breakdown calculates distribution correctly")
    void testPaymentMethodBreakdown() throws Exception {
        LocalDate today = LocalDate.now();
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("8000.00"), today, PaymentMethod.UPI, "UPI spend"));
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("2000.00"), today, PaymentMethod.CASH, "Cash spend"));

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentMethodBreakdown", hasSize(2)))
                .andExpect(jsonPath("$.paymentMethodBreakdown[0].paymentMethod").value("UPI"))
                .andExpect(jsonPath("$.paymentMethodBreakdown[0].totalAmount").value(8000.00))
                .andExpect(jsonPath("$.paymentMethodBreakdown[0].percentage").value(80.00))
                .andExpect(jsonPath("$.paymentMethodBreakdown[1].paymentMethod").value("CASH"))
                .andExpect(jsonPath("$.paymentMethodBreakdown[1].totalAmount").value(2000.00))
                .andExpect(jsonPath("$.paymentMethodBreakdown[1].percentage").value(20.00));
    }

    @Test
    @WithMockUser(username = "alice.dash@example.com")
    @DisplayName("10. Monthly trend returns exactly 6 consecutive months with 0.00 for empty months")
    void testMonthlyTrend_SixMonthsContinuous() throws Exception {
        YearMonth now = YearMonth.now();
        YearMonth twoMonthsAgo = now.minusMonths(2);

        // Expense in current month and 2 months ago; other 4 months empty
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("2500.00"), now.atDay(5), PaymentMethod.UPI, "Now"));
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("1200.00"), twoMonthsAgo.atDay(10), PaymentMethod.CASH, "Two ago"));

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyTrend", hasSize(6)))
                .andExpect(jsonPath("$.monthlyTrend[5].month").value(now.getMonthValue()))
                .andExpect(jsonPath("$.monthlyTrend[5].year").value(now.getYear()))
                .andExpect(jsonPath("$.monthlyTrend[5].totalAmount").value(2500.00))
                .andExpect(jsonPath("$.monthlyTrend[3].month").value(twoMonthsAgo.getMonthValue()))
                .andExpect(jsonPath("$.monthlyTrend[3].totalAmount").value(1200.00))
                .andExpect(jsonPath("$.monthlyTrend[4].totalAmount").value(0.00));
    }

    @Test
    @WithMockUser(username = "alice.dash@example.com")
    @DisplayName("11. Recent expenses returns maximum 5 records sorted date DESC, id DESC")
    void testRecentExpenses_SortedTop5() throws Exception {
        LocalDate baseDate = LocalDate.now();
        for (int i = 1; i <= 7; i++) {
            expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal(i * 100), baseDate.minusDays(7 - i), PaymentMethod.UPI, "Expense " + i));
        }

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recentExpenses", hasSize(5)))
                .andExpect(jsonPath("$.recentExpenses[0].description").value("Expense 7"))
                .andExpect(jsonPath("$.recentExpenses[1].description").value("Expense 6"))
                .andExpect(jsonPath("$.recentExpenses[2].description").value("Expense 5"))
                .andExpect(jsonPath("$.recentExpenses[3].description").value("Expense 4"))
                .andExpect(jsonPath("$.recentExpenses[4].description").value("Expense 3"));
    }

    @Test
    @WithMockUser(username = "alice.dash@example.com")
    @DisplayName("12. Rule-based Insight: NO_DATA when no expenses exist")
    void testInsight_NoData() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.financialInsights[0].type").value("NO_DATA"))
                .andExpect(jsonPath("$.financialInsights[0].title").value("No Expenses Yet"));
    }

    @Test
    @WithMockUser(username = "alice.dash@example.com")
    @DisplayName("13. Rule-based Insight: UNDER_BUDGET when utilization < 80%")
    void testInsight_UnderBudget() throws Exception {
        YearMonth now = YearMonth.now();
        budgetRepository.save(new Budget(userAlice, now.getMonthValue(), now.getYear(), new BigDecimal("10000.00")));
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("5000.00"), now.atDay(5), PaymentMethod.UPI, "Under"));

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.financialInsights[?(@.type == 'UNDER_BUDGET')].title").value("Budget Under Control"));
    }

    @Test
    @WithMockUser(username = "alice.dash@example.com")
    @DisplayName("14. Rule-based Insight: NEAR_LIMIT when 80% <= utilization <= 100%")
    void testInsight_NearLimit() throws Exception {
        YearMonth now = YearMonth.now();
        budgetRepository.save(new Budget(userAlice, now.getMonthValue(), now.getYear(), new BigDecimal("10000.00")));
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("8500.00"), now.atDay(5), PaymentMethod.UPI, "Near"));

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.financialInsights[?(@.type == 'NEAR_LIMIT')].title").value("Budget Near Limit"));
    }

    @Test
    @WithMockUser(username = "alice.dash@example.com")
    @DisplayName("15. Rule-based Insight: OVER_BUDGET when utilization > 100%")
    void testInsight_OverBudget() throws Exception {
        YearMonth now = YearMonth.now();
        budgetRepository.save(new Budget(userAlice, now.getMonthValue(), now.getYear(), new BigDecimal("10000.00")));
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("12500.00"), now.atDay(5), PaymentMethod.UPI, "Over"));

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.financialInsights[?(@.type == 'OVER_BUDGET')].title").value("Budget Exceeded"));
    }

    @Test
    @WithMockUser(username = "alice.dash@example.com")
    @DisplayName("16. Rule-based Insight: CATEGORY_CONCENTRATION when one category > 50%")
    void testInsight_CategoryConcentration() throws Exception {
        LocalDate today = LocalDate.now();
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("7000.00"), today, PaymentMethod.UPI, "Food"));
        expenseRepository.save(new Expense(userAlice, categoryTravel, new BigDecimal("3000.00"), today, PaymentMethod.CARD, "Travel"));

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.financialInsights[?(@.type == 'CATEGORY_CONCENTRATION')].title").value("High Category Spending"));
    }

    @Test
    @WithMockUser(username = "alice.dash@example.com")
    @DisplayName("17. Rule-based Insight: SPENDING_INCREASED when current month > previous month")
    void testInsight_SpendingIncreased() throws Exception {
        YearMonth now = YearMonth.now();
        YearMonth prev = now.minusMonths(1);

        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("2000.00"), prev.atDay(10), PaymentMethod.CASH, "Last month"));
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("5000.00"), now.atDay(10), PaymentMethod.UPI, "This month"));

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.financialInsights[?(@.type == 'SPENDING_INCREASED')].title").value("Spending Increased"));
    }

    @Test
    @WithMockUser(username = "alice.dash@example.com")
    @DisplayName("18. Rule-based Insight: SPENDING_REDUCED when current month < previous month")
    void testInsight_SpendingReduced() throws Exception {
        YearMonth now = YearMonth.now();
        YearMonth prev = now.minusMonths(1);

        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("8000.00"), prev.atDay(10), PaymentMethod.CASH, "Last month"));
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("3000.00"), now.atDay(10), PaymentMethod.UPI, "This month"));

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.financialInsights[?(@.type == 'SPENDING_REDUCED')].title").value("Spending Reduced"));
    }

    @Test
    @WithMockUser(username = "bob.dash@example.com")
    @DisplayName("19. Multi-user data isolation: Bob sees none of Alice's financial records")
    void testMultiUserDataIsolation() throws Exception {
        LocalDate today = LocalDate.now();
        YearMonth now = YearMonth.now();

        // Alice's data
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("5000.00"), today, PaymentMethod.UPI, "Alice Expense"));
        budgetRepository.save(new Budget(userAlice, now.getMonthValue(), now.getYear(), new BigDecimal("20000.00")));

        // Bob has no expenses and no budget
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpenses").value(0.00))
                .andExpect(jsonPath("$.expenseCount").value(0))
                .andExpect(jsonPath("$.currentMonthBudget").doesNotExist())
                .andExpect(jsonPath("$.categoryBreakdown", hasSize(0)))
                .andExpect(jsonPath("$.paymentMethodBreakdown", hasSize(0)))
                .andExpect(jsonPath("$.recentExpenses", hasSize(0)))
                .andExpect(jsonPath("$.financialInsights[0].type").value("NO_DATA"));
    }

    @Test
    @WithMockUser(username = "alice.dash@example.com")
    @DisplayName("20. Single expense scenario behaves deterministically")
    void testSingleExpenseScenario() throws Exception {
        LocalDate today = LocalDate.now();
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("123.45"), today, PaymentMethod.CASH, "Solo expense"));

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpenses").value(123.45))
                .andExpect(jsonPath("$.expenseCount").value(1))
                .andExpect(jsonPath("$.averageExpense").value(123.45))
                .andExpect(jsonPath("$.highestExpense").value(123.45))
                .andExpect(jsonPath("$.categoryBreakdown[0].percentage").value(100.00))
                .andExpect(jsonPath("$.paymentMethodBreakdown[0].percentage").value(100.00));
    }
}
