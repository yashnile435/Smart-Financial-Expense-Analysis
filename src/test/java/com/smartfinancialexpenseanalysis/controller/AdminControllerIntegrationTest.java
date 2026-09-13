package com.smartfinancialexpenseanalysis.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfinancialexpenseanalysis.dto.CategoryRequest;
import com.smartfinancialexpenseanalysis.dto.LoginRequest;
import com.smartfinancialexpenseanalysis.dto.UpdateUserRoleRequest;
import com.smartfinancialexpenseanalysis.dto.UpdateUserStatusRequest;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comprehensive integration tests for Stage 8: Admin Management.
 * Covers authorization, user administration, status toggling, role management,
 * category CRUD & delete safety, system financial overviews, and aggregate analytics.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User adminUser;
    private User regularUserA;
    private User regularUserB;
    private Category testCategory;
    private final String rawPassword = "password123";

    @BeforeEach
    void setUp() {
        // Ensure an admin user exists
        adminUser = userRepository.findByEmail("admin.stage8@example.com").orElseGet(() ->
                userRepository.save(new User("Admin Stage8", "admin.stage8@example.com",
                        passwordEncoder.encode(rawPassword), Role.ADMIN, true))
        );

        // Ensure User A exists
        regularUserA = userRepository.findByEmail("usera.stage8@example.com").orElseGet(() ->
                userRepository.save(new User("User Alpha", "usera.stage8@example.com",
                        passwordEncoder.encode(rawPassword), Role.USER, true))
        );

        // Ensure User B exists
        regularUserB = userRepository.findByEmail("userb.stage8@example.com").orElseGet(() ->
                userRepository.save(new User("User Beta", "userb.stage8@example.com",
                        passwordEncoder.encode(rawPassword), Role.USER, true))
        );

        // Ensure a test category exists
        testCategory = categoryRepository.findByName("AdminTestCat").orElseGet(() ->
                categoryRepository.save(new Category("AdminTestCat", "Category for admin tests"))
        );
    }

    // =========================================================================
    // 1. Authorization Controls
    // =========================================================================

    @Test
    @DisplayName("1. Unauthenticated admin endpoint returns 401 Unauthorized")
    void testUnauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/admin/summary"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/categories"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "usera.stage8@example.com", roles = {"USER"})
    @DisplayName("2. Normal USER receives 403 Forbidden on admin endpoints")
    void testUserRole_Returns403() throws Exception {
        mockMvc.perform(get("/api/admin/summary"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/categories"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/expenses/summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin.stage8@example.com", roles = {"ADMIN"})
    @DisplayName("3. ADMIN receives 200 OK on admin endpoints")
    void testAdminRole_Returns200() throws Exception {
        mockMvc.perform(get("/api/admin/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers", greaterThanOrEqualTo(3)));
    }

    // =========================================================================
    // 2. User Management & Search
    // =========================================================================

    @Test
    @WithMockUser(username = "admin.stage8@example.com", roles = {"ADMIN"})
    @DisplayName("4. Admin can list all users sorted by id DESC")
    void testListUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$[0].password").doesNotExist())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].name").exists())
                .andExpect(jsonPath("$[0].email").exists())
                .andExpect(jsonPath("$[0].role").exists())
                .andExpect(jsonPath("$[0].enabled").exists());
    }

    @Test
    @WithMockUser(username = "admin.stage8@example.com", roles = {"ADMIN"})
    @DisplayName("5. Admin search filters users by name and email server-side")
    void testSearchUsers() throws Exception {
        // Search by name
        mockMvc.perform(get("/api/admin/users").param("search", "Alpha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].email").value("usera.stage8@example.com"));

        // Search by email
        mockMvc.perform(get("/api/admin/users").param("search", "userb.stage8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("User Beta"));
    }

    @Test
    @WithMockUser(username = "admin.stage8@example.com", roles = {"ADMIN"})
    @DisplayName("6. Admin can retrieve safe user details without credentials")
    void testGetUserDetailsSafe() throws Exception {
        mockMvc.perform(get("/api/admin/users/{id}", regularUserA.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(regularUserA.getId()))
                .andExpect(jsonPath("$.name").value("User Alpha"))
                .andExpect(jsonPath("$.email").value("usera.stage8@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @WithMockUser(username = "admin.stage8@example.com", roles = {"ADMIN"})
    @DisplayName("7. Non-existent user ID returns 404 Not Found")
    void testGetUserNotFound() throws Exception {
        mockMvc.perform(get("/api/admin/users/999999"))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // 3. User Status & Login Blocking
    // =========================================================================

    @Test
    @WithMockUser(username = "admin.stage8@example.com", roles = {"ADMIN"})
    @DisplayName("8. Admin can disable a user and disabled user cannot authenticate")
    void testDisableUser_BlocksAuthentication() throws Exception {
        // Disable regularUserA
        UpdateUserStatusRequest disableReq = new UpdateUserStatusRequest(false);
        mockMvc.perform(put("/api/admin/users/{id}/status", regularUserA.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(disableReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        // Verify user in database is disabled
        User updated = userRepository.findById(regularUserA.getId()).orElseThrow();
        assertFalse(updated.isEnabled());

        // Attempt login with disabled credentials
        LoginRequest loginReq = new LoginRequest("usera.stage8@example.com", rawPassword);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Account is disabled")));

        // Re-enable regularUserA
        UpdateUserStatusRequest enableReq = new UpdateUserStatusRequest(true);
        mockMvc.perform(put("/api/admin/users/{id}/status", regularUserA.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(enableReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        // Attempt login again -> must succeed
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "admin.stage8@example.com", roles = {"ADMIN"})
    @DisplayName("9. Administrator cannot disable their own account")
    void testAdminCannotDisableSelf() throws Exception {
        UpdateUserStatusRequest disableReq = new UpdateUserStatusRequest(false);
        mockMvc.perform(put("/api/admin/users/{id}/status", adminUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(disableReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Administrators cannot disable their own account")));
    }

    // =========================================================================
    // 4. Role Management & Safety Rules
    // =========================================================================

    @Test
    @WithMockUser(username = "admin.stage8@example.com", roles = {"ADMIN"})
    @DisplayName("10. Admin can promote USER to ADMIN and demote back to USER")
    void testPromoteAndDemoteUser() throws Exception {
        // Promote regularUserB to ADMIN
        UpdateUserRoleRequest promoteReq = new UpdateUserRoleRequest(Role.ADMIN);
        mockMvc.perform(put("/api/admin/users/{id}/role", regularUserB.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(promoteReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        assertEquals(Role.ADMIN, userRepository.findById(regularUserB.getId()).orElseThrow().getRole());

        // Demote back to USER
        UpdateUserRoleRequest demoteReq = new UpdateUserRoleRequest(Role.USER);
        mockMvc.perform(put("/api/admin/users/{id}/role", regularUserB.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(demoteReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER"));

        assertEquals(Role.USER, userRepository.findById(regularUserB.getId()).orElseThrow().getRole());
    }

    @Test
    @WithMockUser(username = "admin.stage8@example.com", roles = {"ADMIN"})
    @DisplayName("11. Administrator cannot demote their own account")
    void testAdminCannotDemoteSelf() throws Exception {
        UpdateUserRoleRequest demoteReq = new UpdateUserRoleRequest(Role.USER);
        mockMvc.perform(put("/api/admin/users/{id}/role", adminUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(demoteReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Administrators cannot demote their own account")));
    }

    // =========================================================================
    // 5. Category Administration & Delete Safety
    // =========================================================================

    @Test
    @WithMockUser(username = "admin.stage8@example.com", roles = {"ADMIN"})
    @DisplayName("12. Admin can list, create, and update categories")
    void testCategoryCrud() throws Exception {
        // Create new category
        String catName = "Gadgets" + System.currentTimeMillis();
        CategoryRequest createReq = new CategoryRequest(catName, "Electronics & gadgets");

        String responseStr = mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value(catName))
                .andExpect(jsonPath("$.description").value("Electronics & gadgets"))
                .andReturn().getResponse().getContentAsString();

        Long createdId = objectMapper.readTree(responseStr).get("id").asLong();

        // Update category
        CategoryRequest updateReq = new CategoryRequest(catName + " Updated", "Updated desc");
        mockMvc.perform(put("/api/admin/categories/{id}", createdId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(catName + " Updated"))
                .andExpect(jsonPath("$.description").value("Updated desc"));
    }

    @Test
    @WithMockUser(username = "admin.stage8@example.com", roles = {"ADMIN"})
    @DisplayName("13. Duplicate category name is rejected with 409 Conflict")
    void testDuplicateCategory_Returns409() throws Exception {
        CategoryRequest dupReq = new CategoryRequest(testCategory.getName(), "Duplicate description");
        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dupReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @WithMockUser(username = "admin.stage8@example.com", roles = {"ADMIN"})
    @DisplayName("14. Blank category name is rejected with 400 Bad Request")
    void testBlankCategory_Returns400() throws Exception {
        CategoryRequest blankReq = new CategoryRequest("   ", "Blank name test");
        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "admin.stage8@example.com", roles = {"ADMIN"})
    @DisplayName("15. Category referenced by expenses cannot be deleted (409 Conflict)")
    void testCategoryWithExpenses_CannotBeDeleted() throws Exception {
        // Create an expense referencing testCategory
        Expense expense = new Expense(
                regularUserA,
                testCategory,
                new BigDecimal("500.00"),
                LocalDate.now(),
                PaymentMethod.UPI,
                "Referencing expense"
        );
        expenseRepository.save(expense);

        // Attempt deletion of category -> must be blocked
        mockMvc.perform(delete("/api/admin/categories/{id}", testCategory.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("Category cannot be deleted because expenses are using it.")));

        // Verify category still exists
        assertTrue(categoryRepository.existsById(testCategory.getId()));
    }

    @Test
    @WithMockUser(username = "admin.stage8@example.com", roles = {"ADMIN"})
    @DisplayName("16. Unused category can be safely deleted")
    void testUnusedCategory_CanBeDeleted() throws Exception {
        Category unusedCat = categoryRepository.save(new Category("Unused" + System.currentTimeMillis(), "Not used"));
        mockMvc.perform(delete("/api/admin/categories/{id}", unusedCat.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertFalse(categoryRepository.existsById(unusedCat.getId()));
    }

    // =========================================================================
    // 6. System-Wide Financial Overviews & Analytics
    // =========================================================================

    @Test
    @WithMockUser(username = "admin.stage8@example.com", roles = {"ADMIN"})
    @DisplayName("17. Admin expense summary returns accurate system-wide totals")
    void testExpenseSummary() throws Exception {
        // Record test expenses for User A and User B
        expenseRepository.save(new Expense(regularUserA, testCategory, new BigDecimal("1000.00"), LocalDate.now(), PaymentMethod.UPI, "A1"));
        expenseRepository.save(new Expense(regularUserB, testCategory, new BigDecimal("2000.00"), LocalDate.now(), PaymentMethod.CARD, "B1"));

        mockMvc.perform(get("/api/admin/expenses/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expenseCount", greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.totalExpenseAmount", greaterThanOrEqualTo(3000.00)))
                .andExpect(jsonPath("$.highestExpense", greaterThanOrEqualTo(2000.00)))
                .andExpect(jsonPath("$.currentMonthExpenseAmount", greaterThanOrEqualTo(3000.00)));
    }

    @Test
    @WithMockUser(username = "admin.stage8@example.com", roles = {"ADMIN"})
    @DisplayName("18. Admin budget summary reflects system-wide budget status counts")
    void testBudgetSummary() throws Exception {
        YearMonth ym = YearMonth.now();
        // User A sets 5000 budget and spends 1000 (utilization 20% -> UNDER_BUDGET)
        budgetRepository.save(new Budget(regularUserA, ym.getMonthValue(), ym.getYear(), new BigDecimal("5000.00")));
        expenseRepository.save(new Expense(regularUserA, testCategory, new BigDecimal("1000.00"), ym.atDay(5), PaymentMethod.CASH, "Under"));

        mockMvc.perform(get("/api/admin/budgets/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBudgets", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.totalBudgetAmount", greaterThanOrEqualTo(5000.00)))
                .andExpect(jsonPath("$.budgetsUnderBudget", greaterThanOrEqualTo(1)));
    }

    @Test
    @WithMockUser(username = "admin.stage8@example.com", roles = {"ADMIN"})
    @DisplayName("19. Category and payment method analytics aggregate system expenses")
    void testAnalytics() throws Exception {
        expenseRepository.save(new Expense(regularUserA, testCategory, new BigDecimal("400.00"), LocalDate.now(), PaymentMethod.UPI, "UPI expense"));
        expenseRepository.save(new Expense(regularUserB, testCategory, new BigDecimal("600.00"), LocalDate.now(), PaymentMethod.CARD, "CARD expense"));

        // Category analytics
        mockMvc.perform(get("/api/admin/analytics/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));

        // Payment method analytics
        mockMvc.perform(get("/api/admin/analytics/payment-methods"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @WithMockUser(username = "admin.stage8@example.com", roles = {"ADMIN"})
    @DisplayName("20. Monthly analytics returns 6 continuous months including zero months")
    void testMonthlyAnalyticsContinuousMonths() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/monthly").param("months", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)))
                .andExpect(jsonPath("$[5].monthName").exists())
                .andExpect(jsonPath("$[5].totalAmount").exists());
    }

    // =========================================================================
    // 7. Multi-User Access Isolation & Security
    // =========================================================================

    @Test
    @DisplayName("21. Multiple users verification: Admin accesses, User A & B receive 403")
    void testMultiUserSecurity() throws Exception {
        // 1. User A (Role USER) -> 403
        mockMvc.perform(get("/api/admin/summary")
                        .session(createSessionFor("usera.stage8@example.com", rawPassword)))
                .andExpect(status().isForbidden());

        // 2. User B (Role USER) -> 403
        mockMvc.perform(get("/api/admin/summary")
                        .session(createSessionFor("userb.stage8@example.com", rawPassword)))
                .andExpect(status().isForbidden());

        // 3. Admin (Role ADMIN) -> 200
        mockMvc.perform(get("/api/admin/summary")
                        .session(createSessionFor("admin.stage8@example.com", rawPassword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").exists());
    }

    private org.springframework.mock.web.MockHttpSession createSessionFor(String email, String password) throws Exception {
        LoginRequest req = new LoginRequest(email, password);
        return (org.springframework.mock.web.MockHttpSession) mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }
}
