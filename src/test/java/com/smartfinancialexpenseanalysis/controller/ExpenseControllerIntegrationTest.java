package com.smartfinancialexpenseanalysis.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfinancialexpenseanalysis.dto.ExpenseRequest;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comprehensive integration tests for Stage 4 Expense Management.
 * Validates CRUD, ownership security, searching, filtering, validation, and error handling.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ExpenseControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
    private Category categoryTravel;

    @BeforeEach
    void setUp() {
        // Create test users
        userAlice = userRepository.findByEmail("alice@example.com").orElseGet(() ->
                userRepository.save(new User("Alice", "alice@example.com", "passwordHash", Role.USER))
        );

        userBob = userRepository.findByEmail("bob@example.com").orElseGet(() ->
                userRepository.save(new User("Bob", "bob@example.com", "passwordHash", Role.USER))
        );

        // Ensure test categories exist
        categoryFood = categoryRepository.findByName("Food").orElseGet(() ->
                categoryRepository.save(new Category("Food", "Food expenses"))
        );

        categoryTravel = categoryRepository.findByName("Travel").orElseGet(() ->
                categoryRepository.save(new Category("Travel", "Travel expenses"))
        );
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    @DisplayName("1. Authenticated user can create expense (201 Created)")
    void testCreateExpense_AuthenticatedUser_Success() throws Exception {
        ExpenseRequest request = new ExpenseRequest(
                new BigDecimal("500.00"),
                categoryFood.getId(),
                LocalDate.of(2026, 9, 12),
                PaymentMethod.UPI,
                "Dinner with friends"
        );

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.amount").value(500.00))
                .andExpect(jsonPath("$.category.id").value(categoryFood.getId()))
                .andExpect(jsonPath("$.category.name").value("Food"))
                .andExpect(jsonPath("$.date").value("2026-09-12"))
                .andExpect(jsonPath("$.paymentMethod").value("UPI"))
                .andExpect(jsonPath("$.description").value("Dinner with friends"));
    }

    @Test
    @DisplayName("2. Unauthenticated user cannot create expense (401 Unauthorized)")
    void testCreateExpense_Unauthenticated_Fails401() throws Exception {
        ExpenseRequest request = new ExpenseRequest(
                new BigDecimal("500.00"),
                categoryFood.getId(),
                LocalDate.of(2026, 9, 12),
                PaymentMethod.UPI,
                "Dinner"
        );

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    @DisplayName("3. Authenticated user retrieves ONLY their own expenses")
    void testGetExpenses_ReturnsOnlyOwnExpenses() throws Exception {
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("100.00"), LocalDate.now(), PaymentMethod.CASH, "Alice Item 1"));
        expenseRepository.save(new Expense(userAlice, categoryTravel, new BigDecimal("200.00"), LocalDate.now(), PaymentMethod.CARD, "Alice Item 2"));
        expenseRepository.save(new Expense(userBob, categoryFood, new BigDecimal("999.00"), LocalDate.now(), PaymentMethod.UPI, "Bob Secret Item"));

        mockMvc.perform(get("/api/expenses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].description").value("Alice Item 2"))
                .andExpect(jsonPath("$[1].description").value("Alice Item 1"));
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    @DisplayName("4. User cannot retrieve another user's expense (404 Not Found)")
    void testGetExpenseById_OtherUserExpense_Fails404() throws Exception {
        Expense bobExpense = expenseRepository.save(new Expense(userBob, categoryFood, new BigDecimal("750.00"), LocalDate.now(), PaymentMethod.UPI, "Bob Expense"));

        mockMvc.perform(get("/api/expenses/" + bobExpense.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Expense not found"));
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    @DisplayName("5. User can retrieve their single expense")
    void testGetExpenseById_OwnExpense_Success() throws Exception {
        Expense aliceExpense = expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("350.00"), LocalDate.now(), PaymentMethod.CASH, "Alice Coffee"));

        mockMvc.perform(get("/api/expenses/" + aliceExpense.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(aliceExpense.getId()))
                .andExpect(jsonPath("$.amount").value(350.00))
                .andExpect(jsonPath("$.description").value("Alice Coffee"));
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    @DisplayName("6. User can update their expense")
    void testUpdateExpense_OwnExpense_Success() throws Exception {
        Expense expense = expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("150.00"), LocalDate.now(), PaymentMethod.CASH, "Initial Note"));

        ExpenseRequest updateRequest = new ExpenseRequest(
                new BigDecimal("220.00"),
                categoryTravel.getId(),
                LocalDate.of(2026, 9, 15),
                PaymentMethod.CARD,
                "Updated Travel Note"
        );

        mockMvc.perform(put("/api/expenses/" + expense.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(expense.getId()))
                .andExpect(jsonPath("$.amount").value(220.00))
                .andExpect(jsonPath("$.category.name").value("Travel"))
                .andExpect(jsonPath("$.paymentMethod").value("CARD"))
                .andExpect(jsonPath("$.description").value("Updated Travel Note"));
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    @DisplayName("7. User cannot update another user's expense (404 Not Found)")
    void testUpdateExpense_OtherUserExpense_Fails404() throws Exception {
        Expense bobExpense = expenseRepository.save(new Expense(userBob, categoryFood, new BigDecimal("500.00"), LocalDate.now(), PaymentMethod.CASH, "Bob's Expense"));

        ExpenseRequest hackAttempt = new ExpenseRequest(
                new BigDecimal("1.00"),
                categoryFood.getId(),
                LocalDate.now(),
                PaymentMethod.CASH,
                "Hacked"
        );

        mockMvc.perform(put("/api/expenses/" + bobExpense.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hackAttempt)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));

        // Verify Bob's expense was not modified
        Expense unmodified = expenseRepository.findById(bobExpense.getId()).orElseThrow();
        assertFalse(unmodified.getAmount().compareTo(new BigDecimal("1.00")) == 0);
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    @DisplayName("8. User can delete their expense")
    void testDeleteExpense_OwnExpense_Success() throws Exception {
        Expense expense = expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("80.00"), LocalDate.now(), PaymentMethod.CASH, "To delete"));

        mockMvc.perform(delete("/api/expenses/" + expense.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Expense deleted successfully"));

        assertTrue(expenseRepository.findById(expense.getId()).isEmpty());
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    @DisplayName("9. User cannot delete another user's expense (404 Not Found)")
    void testDeleteExpense_OtherUserExpense_Fails404() throws Exception {
        Expense bobExpense = expenseRepository.save(new Expense(userBob, categoryFood, new BigDecimal("80.00"), LocalDate.now(), PaymentMethod.CASH, "Bob item"));

        mockMvc.perform(delete("/api/expenses/" + bobExpense.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));

        assertTrue(expenseRepository.findById(bobExpense.getId()).isPresent());
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    @DisplayName("10. Zero amount rejected (400 Bad Request)")
    void testCreateExpense_ZeroAmount_Fails400() throws Exception {
        ExpenseRequest request = new ExpenseRequest(BigDecimal.ZERO, categoryFood.getId(), LocalDate.now(), PaymentMethod.CASH, "Zero");

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    @DisplayName("11. Negative amount rejected (400 Bad Request)")
    void testCreateExpense_NegativeAmount_Fails400() throws Exception {
        ExpenseRequest request = new ExpenseRequest(new BigDecimal("-50.00"), categoryFood.getId(), LocalDate.now(), PaymentMethod.CASH, "Negative");

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    @DisplayName("12. Missing category rejected (400 Bad Request)")
    void testCreateExpense_MissingCategory_Fails400() throws Exception {
        ExpenseRequest request = new ExpenseRequest(new BigDecimal("100.00"), null, LocalDate.now(), PaymentMethod.CASH, "No Category");

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    @DisplayName("13. Nonexistent category rejected (404 Not Found)")
    void testCreateExpense_NonExistentCategory_Fails404() throws Exception {
        ExpenseRequest request = new ExpenseRequest(new BigDecimal("100.00"), 999999L, LocalDate.now(), PaymentMethod.CASH, "Ghost Category");

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Category not found"));
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    @DisplayName("14. Invalid payment method rejected (400 Bad Request)")
    void testCreateExpense_InvalidPaymentMethod_Fails400() throws Exception {
        String invalidPayload = """
                {
                    "amount": 100.00,
                    "categoryId": %d,
                    "date": "2026-09-12",
                    "paymentMethod": "BITCOIN",
                    "description": "Invalid Method"
                }
                """.formatted(categoryFood.getId());

        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    @DisplayName("15. Invalid date range (startDate after endDate) rejected (400 Bad Request)")
    void testGetExpenses_InvalidDateRange_Fails400() throws Exception {
        mockMvc.perform(get("/api/expenses")
                        .param("startDate", "2026-09-20")
                        .param("endDate", "2026-09-10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Start date cannot be after end date"));
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    @DisplayName("16. Search filter works on description")
    void testGetExpenses_SearchFilter_Success() throws Exception {
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("50.00"), LocalDate.now(), PaymentMethod.UPI, "Grocery market"));
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("150.00"), LocalDate.now(), PaymentMethod.UPI, "Italian Restaurant Dinner"));

        mockMvc.perform(get("/api/expenses").param("search", "dinner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].description").value("Italian Restaurant Dinner"));
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    @DisplayName("17. Category filter works")
    void testGetExpenses_CategoryFilter_Success() throws Exception {
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("50.00"), LocalDate.now(), PaymentMethod.CASH, "Food item"));
        expenseRepository.save(new Expense(userAlice, categoryTravel, new BigDecimal("120.00"), LocalDate.now(), PaymentMethod.CARD, "Flight ticket"));

        mockMvc.perform(get("/api/expenses").param("categoryId", categoryTravel.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].description").value("Flight ticket"));
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    @DisplayName("18. Payment method filter works")
    void testGetExpenses_PaymentMethodFilter_Success() throws Exception {
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("50.00"), LocalDate.now(), PaymentMethod.UPI, "UPI Payment"));
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("120.00"), LocalDate.now(), PaymentMethod.CARD, "Card Payment"));

        mockMvc.perform(get("/api/expenses").param("paymentMethod", "UPI"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].description").value("UPI Payment"));
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    @DisplayName("19. Date range filter works")
    void testGetExpenses_DateRangeFilter_Success() throws Exception {
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("40.00"), LocalDate.of(2026, 8, 25), PaymentMethod.CASH, "Old August Item"));
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("60.00"), LocalDate.of(2026, 9, 5), PaymentMethod.CASH, "September In-Range Item"));

        mockMvc.perform(get("/api/expenses")
                        .param("startDate", "2026-09-01")
                        .param("endDate", "2026-09-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].description").value("September In-Range Item"));
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    @DisplayName("20. Combined filters work together")
    void testGetExpenses_CombinedFilters_Success() throws Exception {
        // Alice items
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("100.00"), LocalDate.of(2026, 9, 5), PaymentMethod.UPI, "Dinner at Taj"));
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("100.00"), LocalDate.of(2026, 9, 5), PaymentMethod.CASH, "Dinner Cash"));
        expenseRepository.save(new Expense(userAlice, categoryTravel, new BigDecimal("100.00"), LocalDate.of(2026, 9, 5), PaymentMethod.UPI, "Dinner Travel Taxi"));
        // Bob item that matches all criteria except ownership
        expenseRepository.save(new Expense(userBob, categoryFood, new BigDecimal("100.00"), LocalDate.of(2026, 9, 5), PaymentMethod.UPI, "Dinner at Taj Bob"));

        mockMvc.perform(get("/api/expenses")
                        .param("categoryId", categoryFood.getId().toString())
                        .param("paymentMethod", "UPI")
                        .param("startDate", "2026-09-01")
                        .param("endDate", "2026-09-10")
                        .param("search", "Taj"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].description").value("Dinner at Taj"));
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    @DisplayName("21. Basic pagination returns specified page and size")
    void testGetExpenses_Pagination_Success() throws Exception {
        for (int i = 1; i <= 5; i++) {
            expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal(10 * i), LocalDate.of(2026, 9, i), PaymentMethod.CASH, "Item " + i));
        }

        mockMvc.perform(get("/api/expenses")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].description").value("Item 5")) // Newest first
                .andExpect(jsonPath("$[1].description").value("Item 4"));
    }

    @Test
    @DisplayName("22. Public GET /api/categories returns standard categories")
    void testGetCategories_Success() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    @DisplayName("23. Filter expenses by minAmount and maxAmount")
    void testGetExpenses_MinMaxAmountFilter_Success() throws Exception {
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("50.00"), LocalDate.of(2026, 9, 1), PaymentMethod.CASH, "Small Item"));
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("250.00"), LocalDate.of(2026, 9, 2), PaymentMethod.UPI, "Medium Item"));
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("1000.00"), LocalDate.of(2026, 9, 3), PaymentMethod.BANK_TRANSFER, "Large Item"));

        mockMvc.perform(get("/api/expenses")
                        .param("minAmount", "100.00")
                        .param("maxAmount", "500.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].description").value("Medium Item"));
    }

    @Test
    @WithMockUser(username = "alice@example.com")
    @DisplayName("24. Invalid minAmount > maxAmount returns 400")
    void testGetExpenses_InvalidMinMaxAmount_Returns400() throws Exception {
        mockMvc.perform(get("/api/expenses")
                        .param("minAmount", "500.00")
                        .param("maxAmount", "100.00"))
                .andExpect(status().isBadRequest());
    }
}
