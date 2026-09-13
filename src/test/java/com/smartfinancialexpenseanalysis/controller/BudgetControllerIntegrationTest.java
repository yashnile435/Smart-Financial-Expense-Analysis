package com.smartfinancialexpenseanalysis.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfinancialexpenseanalysis.dto.BudgetRequest;
import com.smartfinancialexpenseanalysis.entity.Budget;
import com.smartfinancialexpenseanalysis.entity.BudgetStatus;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comprehensive integration tests for Stage 5 Budget Management.
 * Verifies budget CRUD, security/ownership, monthly expense calculation at database level,
 * utilization percentages, status rules, and edge cases.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BudgetControllerIntegrationTest {

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

    @Autowired
    private ObjectMapper objectMapper;

    private User userAlice;
    private User userBob;
    private Category categoryFood;

    @BeforeEach
    void setUp() {
        userAlice = userRepository.findByEmail("alice.budget@example.com").orElseGet(() ->
                userRepository.save(new User("Alice Budget", "alice.budget@example.com", "passwordHash", Role.USER))
        );

        userBob = userRepository.findByEmail("bob.budget@example.com").orElseGet(() ->
                userRepository.save(new User("Bob Budget", "bob.budget@example.com", "passwordHash", Role.USER))
        );

        categoryFood = categoryRepository.findByName("Food").orElseGet(() ->
                categoryRepository.save(new Category("Food", "Food expenses"))
        );
    }

    @Test
    @WithMockUser(username = "alice.budget@example.com")
    @DisplayName("1. Authenticated user can create budget (201 Created)")
    void testCreateBudget_AuthenticatedUser_Success() throws Exception {
        BudgetRequest request = new BudgetRequest(9, 2026, new BigDecimal("20000.00"));

        mockMvc.perform(post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.month").value(9))
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.budgetAmount").value(20000.00))
                .andExpect(jsonPath("$.totalExpenses").value(0.00))
                .andExpect(jsonPath("$.remainingAmount").value(20000.00))
                .andExpect(jsonPath("$.utilizationPercentage").value(0.00))
                .andExpect(jsonPath("$.status").value("UNDER_BUDGET"));
    }

    @Test
    @DisplayName("2. Unauthenticated user cannot create budget (401 Unauthorized)")
    void testCreateBudget_Unauthenticated_ReturnsUnauthorized() throws Exception {
        BudgetRequest request = new BudgetRequest(9, 2026, new BigDecimal("20000.00"));

        mockMvc.perform(post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "alice.budget@example.com")
    @DisplayName("3. User can retrieve their budget by ID (200 OK)")
    void testGetBudgetById_Owner_Success() throws Exception {
        Budget budget = budgetRepository.save(new Budget(userAlice, 9, 2026, new BigDecimal("20000.00")));

        mockMvc.perform(get("/api/budgets/" + budget.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(budget.getId()))
                .andExpect(jsonPath("$.month").value(9))
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.budgetAmount").value(20000.00));
    }

    @Test
    @WithMockUser(username = "bob.budget@example.com")
    @DisplayName("4. User cannot retrieve another user's budget (404 Not Found)")
    void testGetBudgetById_NotOwner_ReturnsNotFound() throws Exception {
        Budget aliceBudget = budgetRepository.save(new Budget(userAlice, 9, 2026, new BigDecimal("20000.00")));

        mockMvc.perform(get("/api/budgets/" + aliceBudget.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "alice.budget@example.com")
    @DisplayName("5. User can update their budget (200 OK)")
    void testUpdateBudget_Owner_Success() throws Exception {
        Budget budget = budgetRepository.save(new Budget(userAlice, 9, 2026, new BigDecimal("20000.00")));

        BudgetRequest updateRequest = new BudgetRequest(9, 2026, new BigDecimal("25000.00"));

        mockMvc.perform(put("/api/budgets/" + budget.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(budget.getId()))
                .andExpect(jsonPath("$.budgetAmount").value(25000.00));
    }

    @Test
    @WithMockUser(username = "bob.budget@example.com")
    @DisplayName("6. User cannot update another user's budget (404 Not Found)")
    void testUpdateBudget_NotOwner_ReturnsNotFound() throws Exception {
        Budget aliceBudget = budgetRepository.save(new Budget(userAlice, 9, 2026, new BigDecimal("20000.00")));

        BudgetRequest updateRequest = new BudgetRequest(9, 2026, new BigDecimal("25000.00"));

        mockMvc.perform(put("/api/budgets/" + aliceBudget.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "alice.budget@example.com")
    @DisplayName("7. User can delete their budget (200 OK)")
    void testDeleteBudget_Owner_Success() throws Exception {
        Budget budget = budgetRepository.save(new Budget(userAlice, 9, 2026, new BigDecimal("20000.00")));

        mockMvc.perform(delete("/api/budgets/" + budget.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertFalse(budgetRepository.findById(budget.getId()).isPresent());
    }

    @Test
    @WithMockUser(username = "bob.budget@example.com")
    @DisplayName("8. User cannot delete another user's budget (404 Not Found)")
    void testDeleteBudget_NotOwner_ReturnsNotFound() throws Exception {
        Budget aliceBudget = budgetRepository.save(new Budget(userAlice, 9, 2026, new BigDecimal("20000.00")));

        mockMvc.perform(delete("/api/budgets/" + aliceBudget.getId()))
                .andExpect(status().isNotFound());

        assertTrue(budgetRepository.findById(aliceBudget.getId()).isPresent());
    }

    @Test
    @WithMockUser(username = "alice.budget@example.com")
    @DisplayName("9. Duplicate budget for same user/month/year is rejected (409 Conflict)")
    void testCreateBudget_Duplicate_ReturnsConflict() throws Exception {
        budgetRepository.save(new Budget(userAlice, 9, 2026, new BigDecimal("20000.00")));

        BudgetRequest duplicateRequest = new BudgetRequest(9, 2026, new BigDecimal("25000.00"));

        mockMvc.perform(post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "alice.budget@example.com")
    @DisplayName("10. Invalid month is rejected (400 Bad Request)")
    void testCreateBudget_InvalidMonth_ReturnsBadRequest() throws Exception {
        BudgetRequest request = new BudgetRequest(13, 2026, new BigDecimal("20000.00"));

        mockMvc.perform(post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "alice.budget@example.com")
    @DisplayName("11. Invalid null amount is rejected (400 Bad Request)")
    void testCreateBudget_NullAmount_ReturnsBadRequest() throws Exception {
        BudgetRequest request = new BudgetRequest(9, 2026, null);

        mockMvc.perform(post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "alice.budget@example.com")
    @DisplayName("12. Zero amount is rejected (400 Bad Request)")
    void testCreateBudget_ZeroAmount_ReturnsBadRequest() throws Exception {
        BudgetRequest request = new BudgetRequest(9, 2026, BigDecimal.ZERO);

        mockMvc.perform(post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "alice.budget@example.com")
    @DisplayName("13. Negative amount is rejected (400 Bad Request)")
    void testCreateBudget_NegativeAmount_ReturnsBadRequest() throws Exception {
        BudgetRequest request = new BudgetRequest(9, 2026, new BigDecimal("-500.00"));

        mockMvc.perform(post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "alice.budget@example.com")
    @DisplayName("14. Monthly expense calculation works at database level")
    void testMonthlyExpenseCalculation() throws Exception {
        Budget budget = budgetRepository.save(new Budget(userAlice, 9, 2026, new BigDecimal("20000.00")));

        // Expenses in Sept 2026
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("4500.00"), LocalDate.of(2026, 9, 5), PaymentMethod.UPI, "Groceries"));
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("10000.00"), LocalDate.of(2026, 9, 20), PaymentMethod.CARD, "Electronics"));

        // Expense in Oct 2026 (outside month)
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("3000.00"), LocalDate.of(2026, 10, 1), PaymentMethod.CASH, "Outside month"));

        // Expense by Bob in Sept 2026 (different user)
        expenseRepository.save(new Expense(userBob, categoryFood, new BigDecimal("5000.00"), LocalDate.of(2026, 9, 15), PaymentMethod.UPI, "Bob expense"));

        mockMvc.perform(get("/api/budgets/" + budget.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budgetAmount").value(20000.00))
                .andExpect(jsonPath("$.totalExpenses").value(14500.00))
                .andExpect(jsonPath("$.remainingAmount").value(5500.00))
                .andExpect(jsonPath("$.utilizationPercentage").value(72.50))
                .andExpect(jsonPath("$.status").value("UNDER_BUDGET"));
    }

    @Test
    @WithMockUser(username = "alice.budget@example.com")
    @DisplayName("15. Remaining amount calculation works")
    void testRemainingAmountCalculation() throws Exception {
        Budget budget = budgetRepository.save(new Budget(userAlice, 9, 2026, new BigDecimal("20000.00")));
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("14500.00"), LocalDate.of(2026, 9, 10), PaymentMethod.UPI, "Shopping"));

        mockMvc.perform(get("/api/budgets/" + budget.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingAmount").value(5500.00));
    }

    @Test
    @WithMockUser(username = "alice.budget@example.com")
    @DisplayName("16. Utilization percentage calculation works")
    void testUtilizationPercentageCalculation() throws Exception {
        Budget budget = budgetRepository.save(new Budget(userAlice, 9, 2026, new BigDecimal("20000.00")));
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("14500.00"), LocalDate.of(2026, 9, 10), PaymentMethod.UPI, "Shopping"));

        mockMvc.perform(get("/api/budgets/" + budget.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.utilizationPercentage").value(72.50));
    }

    @Test
    @WithMockUser(username = "alice.budget@example.com")
    @DisplayName("17. Edge Case: Budget 20,000, Expenses 0 -> Remaining 20,000, Utilization 0%, UNDER_BUDGET")
    void testEdgeCase_ZeroExpenses_UnderBudget() throws Exception {
        Budget budget = budgetRepository.save(new Budget(userAlice, 9, 2026, new BigDecimal("20000.00")));

        mockMvc.perform(get("/api/budgets/" + budget.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budgetAmount").value(20000.00))
                .andExpect(jsonPath("$.totalExpenses").value(0.00))
                .andExpect(jsonPath("$.remainingAmount").value(20000.00))
                .andExpect(jsonPath("$.utilizationPercentage").value(0.00))
                .andExpect(jsonPath("$.status").value("UNDER_BUDGET"));
    }

    @Test
    @WithMockUser(username = "alice.budget@example.com")
    @DisplayName("18. Edge Case: Budget 20,000, Expenses 16,000 -> Utilization 80%, NEAR_LIMIT")
    void testEdgeCase_EightyPercent_NearLimit() throws Exception {
        Budget budget = budgetRepository.save(new Budget(userAlice, 9, 2026, new BigDecimal("20000.00")));
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("16000.00"), LocalDate.of(2026, 9, 10), PaymentMethod.UPI, "Large spend"));

        mockMvc.perform(get("/api/budgets/" + budget.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budgetAmount").value(20000.00))
                .andExpect(jsonPath("$.totalExpenses").value(16000.00))
                .andExpect(jsonPath("$.remainingAmount").value(4000.00))
                .andExpect(jsonPath("$.utilizationPercentage").value(80.00))
                .andExpect(jsonPath("$.status").value("NEAR_LIMIT"));
    }

    @Test
    @WithMockUser(username = "alice.budget@example.com")
    @DisplayName("19. Edge Case: Budget 20,000, Expenses 20,000 -> Remaining 0, Utilization 100%, NEAR_LIMIT")
    void testEdgeCase_HundredPercent_NearLimit() throws Exception {
        Budget budget = budgetRepository.save(new Budget(userAlice, 9, 2026, new BigDecimal("20000.00")));
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("20000.00"), LocalDate.of(2026, 9, 10), PaymentMethod.UPI, "Exact limit"));

        mockMvc.perform(get("/api/budgets/" + budget.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budgetAmount").value(20000.00))
                .andExpect(jsonPath("$.totalExpenses").value(20000.00))
                .andExpect(jsonPath("$.remainingAmount").value(0.00))
                .andExpect(jsonPath("$.utilizationPercentage").value(100.00))
                .andExpect(jsonPath("$.status").value("NEAR_LIMIT"));
    }

    @Test
    @WithMockUser(username = "alice.budget@example.com")
    @DisplayName("20. Edge Case: Budget 20,000, Expenses 25,000 -> Remaining -5,000, Utilization 125%, OVER_BUDGET")
    void testEdgeCase_OverBudget_NegativeRemaining() throws Exception {
        Budget budget = budgetRepository.save(new Budget(userAlice, 9, 2026, new BigDecimal("20000.00")));
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("25000.00"), LocalDate.of(2026, 9, 10), PaymentMethod.UPI, "Over budget spend"));

        mockMvc.perform(get("/api/budgets/" + budget.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budgetAmount").value(20000.00))
                .andExpect(jsonPath("$.totalExpenses").value(25000.00))
                .andExpect(jsonPath("$.remainingAmount").value(-5000.00))
                .andExpect(jsonPath("$.utilizationPercentage").value(125.00))
                .andExpect(jsonPath("$.status").value("OVER_BUDGET"));
    }

    @Test
    @WithMockUser(username = "alice.budget@example.com")
    @DisplayName("21. Current month budget works (GET /api/budgets/current)")
    void testGetCurrentMonthBudget_Success() throws Exception {
        LocalDate now = LocalDate.now();
        budgetRepository.save(new Budget(userAlice, now.getMonthValue(), now.getYear(), new BigDecimal("30000.00")));

        mockMvc.perform(get("/api/budgets/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value(now.getMonthValue()))
                .andExpect(jsonPath("$.year").value(now.getYear()))
                .andExpect(jsonPath("$.budgetAmount").value(30000.00));
    }

    @Test
    @WithMockUser(username = "alice.budget@example.com")
    @DisplayName("22. Current month budget returns 404 when no budget configured")
    void testGetCurrentMonthBudget_NotFound() throws Exception {
        // Ensure no budget exists for current month
        LocalDate now = LocalDate.now();
        budgetRepository.findByUserIdAndMonthAndYear(userAlice.getId(), now.getMonthValue(), now.getYear())
                .ifPresent(budgetRepository::delete);

        mockMvc.perform(get("/api/budgets/current"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "alice.budget@example.com")
    @DisplayName("23. Historical budget lookup works (GET /api/budgets?month=9&year=2026)")
    void testGetBudgetByMonthAndYear_Success() throws Exception {
        budgetRepository.save(new Budget(userAlice, 9, 2026, new BigDecimal("15000.00")));

        mockMvc.perform(get("/api/budgets?month=9&year=2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value(9))
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.budgetAmount").value(15000.00));
    }

    @Test
    @WithMockUser(username = "alice.budget@example.com")
    @DisplayName("24. Budget history only contains current user's budgets (GET /api/budgets/all)")
    void testGetAllBudgets_ReturnsOnlyCurrentUserData_Sorted() throws Exception {
        budgetRepository.save(new Budget(userAlice, 8, 2026, new BigDecimal("10000.00")));
        budgetRepository.save(new Budget(userAlice, 9, 2026, new BigDecimal("20000.00")));
        budgetRepository.save(new Budget(userBob, 9, 2026, new BigDecimal("50000.00"))); // Bob's budget

        mockMvc.perform(get("/api/budgets/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].month").value(9))
                .andExpect(jsonPath("$[0].year").value(2026))
                .andExpect(jsonPath("$[1].month").value(8))
                .andExpect(jsonPath("$[1].year").value(2026));
    }

    @Test
    @WithMockUser(username = "alice.budget@example.com")
    @DisplayName("25. Date calculation handles leap year accurately (February 2024)")
    void testDateCalculation_LeapYearHandling() throws Exception {
        Budget budget = budgetRepository.save(new Budget(userAlice, 2, 2024, new BigDecimal("10000.00")));

        // Feb 29 on leap year 2024
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("2500.00"), LocalDate.of(2024, 2, 29), PaymentMethod.UPI, "Leap day"));
        // March 1 (next day, excluded)
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("1000.00"), LocalDate.of(2024, 3, 1), PaymentMethod.UPI, "March 1"));

        mockMvc.perform(get("/api/budgets/" + budget.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpenses").value(2500.00))
                .andExpect(jsonPath("$.remainingAmount").value(7500.00))
                .andExpect(jsonPath("$.utilizationPercentage").value(25.00));
    }

    @Test
    @WithMockUser(username = "alice.budget@example.com")
    @DisplayName("26. Invalid year (< 2000) is rejected")
    void testCreateBudget_InvalidYear_ReturnsBadRequest() throws Exception {
        BudgetRequest request = new BudgetRequest(9, 1999, new BigDecimal("20000.00"));

        mockMvc.perform(post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "alice.budget@example.com")
    @DisplayName("27. USER can edit own budget amount and changes are persisted in MySQL")
    void testUpdateBudget_AmountPersistedInDatabase() throws Exception {
        Budget budget = budgetRepository.save(new Budget(userAlice, 3, 2026, new BigDecimal("10000.00")));

        BudgetRequest updateRequest = new BudgetRequest(3, 2026, new BigDecimal("15000.00"));

        mockMvc.perform(put("/api/budgets/" + budget.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(budget.getId()))
                .andExpect(jsonPath("$.budgetAmount").value(15000.00));

        Budget persisted = budgetRepository.findById(budget.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("15000.00").compareTo(persisted.getAmount()));
        assertEquals(3, persisted.getMonth());
        assertEquals(2026, persisted.getYear());
    }

    @Test
    @WithMockUser(username = "alice.budget@example.com")
    @DisplayName("28. USER can edit month/year when no duplicate exists")
    void testUpdateBudget_ChangeMonthAndYear_Success() throws Exception {
        Budget budget = budgetRepository.save(new Budget(userAlice, 3, 2026, new BigDecimal("10000.00")));

        BudgetRequest updateRequest = new BudgetRequest(5, 2026, new BigDecimal("12000.00"));

        mockMvc.perform(put("/api/budgets/" + budget.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(budget.getId()))
                .andExpect(jsonPath("$.month").value(5))
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.budgetAmount").value(12000.00));

        Budget persisted = budgetRepository.findById(budget.getId()).orElseThrow();
        assertEquals(5, persisted.getMonth());
        assertEquals(2026, persisted.getYear());
        assertEquals(0, new BigDecimal("12000.00").compareTo(persisted.getAmount()));
    }

    @Test
    @WithMockUser(username = "alice.budget@example.com")
    @DisplayName("29. Editing same month/year does not trigger false duplicate error")
    void testUpdateBudget_SameMonthYear_DoesNotTriggerDuplicate() throws Exception {
        Budget budget = budgetRepository.save(new Budget(userAlice, 3, 2026, new BigDecimal("10000.00")));

        BudgetRequest updateRequest = new BudgetRequest(3, 2026, new BigDecimal("18000.00"));

        mockMvc.perform(put("/api/budgets/" + budget.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budgetAmount").value(18000.00));
    }

    @Test
    @WithMockUser(username = "alice.budget@example.com")
    @DisplayName("30. Editing to an already-existing month/year for user is rejected (409 Conflict)")
    void testUpdateBudget_ExistingMonthYear_ReturnsConflict() throws Exception {
        Budget marchBudget = budgetRepository.save(new Budget(userAlice, 3, 2026, new BigDecimal("10000.00")));
        budgetRepository.save(new Budget(userAlice, 4, 2026, new BigDecimal("12000.00")));

        // Attempt to edit March budget to April 2026
        BudgetRequest updateRequest = new BudgetRequest(4, 2026, new BigDecimal("15000.00"));

        mockMvc.perform(put("/api/budgets/" + marchBudget.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));

        // Confirm March budget was NOT changed
        Budget unchanged = budgetRepository.findById(marchBudget.getId()).orElseThrow();
        assertEquals(3, unchanged.getMonth());
        assertEquals(0, new BigDecimal("10000.00").compareTo(unchanged.getAmount()));
    }

    @Test
    @WithMockUser(username = "bob.budget@example.com")
    @DisplayName("31. USER cannot edit another user's budget (404 Not Found, IDOR blocked)")
    void testUpdateBudget_OtherUserBudget_ReturnsNotFound() throws Exception {
        Budget aliceBudget = budgetRepository.save(new Budget(userAlice, 3, 2026, new BigDecimal("10000.00")));

        BudgetRequest updateRequest = new BudgetRequest(3, 2026, new BigDecimal("50000.00"));

        mockMvc.perform(put("/api/budgets/" + aliceBudget.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));

        Budget unchanged = budgetRepository.findById(aliceBudget.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("10000.00").compareTo(unchanged.getAmount()));
    }

    @Test
    @DisplayName("32. Unauthenticated PUT is rejected (401 Unauthorized)")
    void testUpdateBudget_Unauthenticated_ReturnsUnauthorized() throws Exception {
        Budget budget = budgetRepository.save(new Budget(userAlice, 3, 2026, new BigDecimal("10000.00")));

        BudgetRequest updateRequest = new BudgetRequest(3, 2026, new BigDecimal("15000.00"));

        mockMvc.perform(put("/api/budgets/" + budget.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "alice.budget@example.com")
    @DisplayName("33. Invalid amount in PUT (zero or negative or null) is rejected (400 Bad Request)")
    void testUpdateBudget_InvalidAmount_ReturnsBadRequest() throws Exception {
        Budget budget = budgetRepository.save(new Budget(userAlice, 3, 2026, new BigDecimal("10000.00")));

        BudgetRequest zeroRequest = new BudgetRequest(3, 2026, BigDecimal.ZERO);
        mockMvc.perform(put("/api/budgets/" + budget.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(zeroRequest)))
                .andExpect(status().isBadRequest());

        BudgetRequest negativeRequest = new BudgetRequest(3, 2026, new BigDecimal("-100.00"));
        mockMvc.perform(put("/api/budgets/" + budget.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(negativeRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "alice.budget@example.com")
    @DisplayName("34. Updated remaining amount, utilization percentage, and status are recalculated correctly")
    void testUpdateBudget_RecalculatesMetricsCorrectly() throws Exception {
        Budget budget = budgetRepository.save(new Budget(userAlice, 3, 2026, new BigDecimal("10000.00")));
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("12000.00"), LocalDate.of(2026, 3, 15), PaymentMethod.UPI, "Groceries"));

        // Before update: 12000 / 10000 = 120%, OVER_BUDGET
        mockMvc.perform(get("/api/budgets/" + budget.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingAmount").value(-2000.00))
                .andExpect(jsonPath("$.utilizationPercentage").value(120.00))
                .andExpect(jsonPath("$.status").value("OVER_BUDGET"));

        // Update amount to 20,000.00 -> 12,000 spent -> remaining 8,000, 60.00%, UNDER_BUDGET
        BudgetRequest updateRequest = new BudgetRequest(3, 2026, new BigDecimal("20000.00"));
        mockMvc.perform(put("/api/budgets/" + budget.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budgetAmount").value(20000.00))
                .andExpect(jsonPath("$.totalExpenses").value(12000.00))
                .andExpect(jsonPath("$.remainingAmount").value(8000.00))
                .andExpect(jsonPath("$.utilizationPercentage").value(60.00))
                .andExpect(jsonPath("$.status").value("UNDER_BUDGET"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("35. ADMIN cannot access or edit personal budget endpoint (403 Forbidden)")
    void testUpdateBudget_AdminRole_Forbidden() throws Exception {
        Budget aliceBudget = budgetRepository.save(new Budget(userAlice, 3, 2026, new BigDecimal("10000.00")));

        BudgetRequest updateRequest = new BudgetRequest(3, 2026, new BigDecimal("20000.00"));

        mockMvc.perform(put("/api/budgets/" + aliceBudget.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "alice.budget@example.com")
    @DisplayName("36. Invalid month (<1 or >12) or year (<2000) in PUT is rejected (400 Bad Request)")
    void testUpdateBudget_InvalidMonthYear_ReturnsBadRequest() throws Exception {
        Budget budget = budgetRepository.save(new Budget(userAlice, 3, 2026, new BigDecimal("10000.00")));

        BudgetRequest badMonthRequest = new BudgetRequest(13, 2026, new BigDecimal("15000.00"));
        mockMvc.perform(put("/api/budgets/" + budget.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badMonthRequest)))
                .andExpect(status().isBadRequest());

        BudgetRequest badYearRequest = new BudgetRequest(3, 1999, new BigDecimal("15000.00"));
        mockMvc.perform(put("/api/budgets/" + budget.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badYearRequest)))
                .andExpect(status().isBadRequest());
    }
}
