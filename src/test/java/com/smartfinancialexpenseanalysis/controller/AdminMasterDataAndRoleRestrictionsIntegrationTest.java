package com.smartfinancialexpenseanalysis.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfinancialexpenseanalysis.dto.CategoryRequest;
import com.smartfinancialexpenseanalysis.dto.ExpenseRequest;
import com.smartfinancialexpenseanalysis.dto.PaymentOptionRequest;
import com.smartfinancialexpenseanalysis.entity.Category;
import com.smartfinancialexpenseanalysis.entity.Expense;
import com.smartfinancialexpenseanalysis.entity.PaymentMethod;
import com.smartfinancialexpenseanalysis.entity.PaymentOption;
import com.smartfinancialexpenseanalysis.entity.Role;
import com.smartfinancialexpenseanalysis.entity.User;
import com.smartfinancialexpenseanalysis.repository.CategoryRepository;
import com.smartfinancialexpenseanalysis.repository.ExpenseRepository;
import com.smartfinancialexpenseanalysis.repository.PaymentOptionRepository;
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

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for:
 * 1. Admin Category & Payment Option Master Data Management
 * 2. Strict Role Boundary Enforcement (ADMIN forbidden on personal features, USER forbidden on admin features)
 * 3. Dynamic Payment Option creation and expense association
 * 4. Referential integrity & safe deactivation
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminMasterDataAndRoleRestrictionsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PaymentOptionRepository paymentOptionRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private User testAdmin;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testUser = userRepository.findByEmail("normal.user@example.com")
                .orElseGet(() -> userRepository.save(new User(
                        "Normal User",
                        "normal.user@example.com",
                        passwordEncoder.encode("Password@123"),
                        Role.USER,
                        true
                )));

        testAdmin = userRepository.findByEmail("admin.master@example.com")
                .orElseGet(() -> userRepository.save(new User(
                        "Master Admin",
                        "admin.master@example.com",
                        passwordEncoder.encode("Admin@123"),
                        Role.ADMIN,
                        true
                )));

        testCategory = categoryRepository.findByNameIgnoreCase("Utilities")
                .orElseGet(() -> categoryRepository.save(new Category("Utilities", "Utility bills and energy")));
    }

    // =========================================================================
    // 1. Category Master Data Management (ADMIN & USER)
    // =========================================================================

    @Test
    @WithMockUser(username = "admin.master@example.com", roles = {"ADMIN"})
    @DisplayName("Admin can create, update, and delete a category safely")
    void testAdminCategoryCrud() throws Exception {
        String catName = "Transport_" + System.currentTimeMillis();
        CategoryRequest createReq = new CategoryRequest(catName, "Public and private transit");

        // 1. Admin creates category
        String createRes = mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(catName))
                .andReturn().getResponse().getContentAsString();

        Long categoryId = objectMapper.readTree(createRes).get("id").asLong();

        // 2. Duplicate category rejected (409)
        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isConflict());

        // 3. Admin updates category
        CategoryRequest updateReq = new CategoryRequest(catName + " Updated", "Updated transit info");
        mockMvc.perform(put("/api/admin/categories/{id}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(catName + " Updated"));

        // 4. Admin deletes unreferenced category
        mockMvc.perform(delete("/api/admin/categories/{id}", categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "admin.master@example.com", roles = {"ADMIN"})
    @DisplayName("Admin cannot delete category if referenced by expenses (409 Conflict)")
    void testAdminCannotDeleteReferencedCategory() throws Exception {
        Category category = categoryRepository.save(new Category("ReferencedCat_" + System.currentTimeMillis(), "Desc"));
        expenseRepository.save(new Expense(
                testUser,
                category,
                new BigDecimal("150.00"),
                LocalDate.now(),
                PaymentMethod.UPI,
                "Taxi ride"
        ));

        mockMvc.perform(delete("/api/admin/categories/{id}", category.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "normal.user@example.com", roles = {"USER"})
    @DisplayName("USER can read categories but cannot create or modify categories")
    void testUserCategoryAccess() throws Exception {
        // USER can read categories
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // USER cannot access admin category creation (403 Forbidden)
        CategoryRequest req = new CategoryRequest("HackingCategory", "Not allowed");
        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 2. Payment Option Master Data Management (ADMIN & USER)
    // =========================================================================

    @Test
    @WithMockUser(username = "admin.master@example.com", roles = {"ADMIN"})
    @DisplayName("Admin can create, update, toggle, and manage payment options")
    void testAdminPaymentOptionCrud() throws Exception {
        String optName = "Crypto_" + System.currentTimeMillis();
        PaymentOptionRequest createReq = new PaymentOptionRequest(optName, "Cryptocurrency payments", true);

        // 1. Create payment option
        String createRes = mockMvc.perform(post("/api/admin/payment-options")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(optName))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn().getResponse().getContentAsString();

        Long optId = objectMapper.readTree(createRes).get("id").asLong();

        // 2. Duplicate payment option name rejected (409 Conflict)
        mockMvc.perform(post("/api/admin/payment-options")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isConflict());

        // 3. Update payment option
        PaymentOptionRequest updateReq = new PaymentOptionRequest(optName, "Updated crypto description", true);
        mockMvc.perform(put("/api/admin/payment-options/{id}", optId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated crypto description"));

        // 4. Toggle status to inactive
        mockMvc.perform(patch("/api/admin/payment-options/{id}/status?active=false", optId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        assertFalse(paymentOptionRepository.findById(optId).orElseThrow().isActive());

        // 5. Delete unreferenced payment option permanently
        mockMvc.perform(delete("/api/admin/payment-options/{id}", optId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertFalse(paymentOptionRepository.existsById(optId));
    }

    @Test
    @WithMockUser(username = "admin.master@example.com", roles = {"ADMIN"})
    @DisplayName("Admin deleting a payment option referenced by expenses deactivates it safely instead of hard deleting")
    void testSafeDeactivationOfReferencedPaymentOption() throws Exception {
        String optName = "Cheque_" + System.currentTimeMillis();
        PaymentOption option = paymentOptionRepository.save(new PaymentOption(optName, "Cheque payments", true));

        // Create an expense referencing this payment option
        expenseRepository.save(new Expense(
                testUser,
                testCategory,
                new BigDecimal("5000.00"),
                LocalDate.now(),
                PaymentMethod.of(optName),
                "Cheque clearing"
        ));

        // Attempt delete by admin
        mockMvc.perform(delete("/api/admin/payment-options/{id}", option.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Verify entity still exists in database but active is now false
        PaymentOption found = paymentOptionRepository.findById(option.getId()).orElseThrow();
        assertFalse(found.isActive(), "Referenced payment option must be deactivated rather than deleted");
    }

    @Test
    @WithMockUser(username = "normal.user@example.com", roles = {"USER"})
    @DisplayName("USER can read active payment options but cannot modify payment options")
    void testUserPaymentOptionAccess() throws Exception {
        // Read active payment options
        mockMvc.perform(get("/api/payment-options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Modification forbidden for USER (403)
        PaymentOptionRequest req = new PaymentOptionRequest("IllegalOption", "Test", true);
        mockMvc.perform(post("/api/admin/payment-options")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // 3. Dynamic Payment Option in Expense Lifecycle
    // =========================================================================

    @Test
    @DisplayName("Dynamic payment option creation by ADMIN and usage by USER in expense creation")
    void testDynamicPaymentOptionExpenseWorkflow() throws Exception {
        String customOptionName = "Digital Wallet " + System.currentTimeMillis();

        // 1. Admin creates "Digital Wallet"
        PaymentOption customOption = paymentOptionRepository.save(new PaymentOption(customOptionName, "E-wallets", true));

        // 2. User checks GET /api/payment-options and verifies it is present
        mockMvc.perform(get("/api/payment-options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem(customOptionName)));

        // 3. User creates an expense using "Digital Wallet"
        ExpenseRequest expenseReq = new ExpenseRequest(
                new BigDecimal("250.00"),
                testCategory.getId(),
                LocalDate.now(),
                PaymentMethod.of(customOptionName),
                "Groceries with digital wallet"
        );

        mockMvc.perform(post("/api/expenses")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("normal.user@example.com").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expenseReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentMethod").value(customOptionName));

        // 4. Admin deactivates the option
        customOption.setActive(false);
        paymentOptionRepository.save(customOption);

        // 5. Active payment options list no longer includes deactivated option
        mockMvc.perform(get("/api/payment-options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", not(hasItem(customOptionName))));

        // 6. User attempting to create a new expense with deactivated option receives 400 Bad Request
        mockMvc.perform(post("/api/expenses")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("normal.user@example.com").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(expenseReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("is inactive")));
    }

    // =========================================================================
    // 4. Strict Role Boundary Enforcement
    // =========================================================================

    @Test
    @WithMockUser(username = "normal.user@example.com", roles = {"USER"})
    @DisplayName("USER is strictly forbidden (403) from all admin endpoints")
    void testUserCannotAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/users")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/summary")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/analytics/categories")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/categories")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/payment-options")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin.master@example.com", roles = {"ADMIN"})
    @DisplayName("ADMIN is strictly forbidden (403) from personal financial endpoints")
    void testAdminCannotAccessPersonalFinancialEndpoints() throws Exception {
        // Expenses
        mockMvc.perform(get("/api/expenses")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        // Budgets
        mockMvc.perform(get("/api/budgets")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/budgets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        // Goals
        mockMvc.perform(get("/api/goals")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        // Recurring Expenses
        mockMvc.perform(get("/api/recurring-expenses")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/recurring-expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        // Dashboard Summary
        mockMvc.perform(get("/api/dashboard/summary")).andExpect(status().isForbidden());

        // Reports
        mockMvc.perform(get("/api/reports/monthly?year=2026&month=9")).andExpect(status().isForbidden());

        // Analytics
        mockMvc.perform(get("/api/analytics/spending-trends?months=6")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Both USER and ADMIN can manage their own profile")
    void testProfileAccessForBothRoles() throws Exception {
        // USER can access profile
        mockMvc.perform(get("/api/profile")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("normal.user@example.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("normal.user@example.com"));

        // ADMIN can access profile
        mockMvc.perform(get("/api/profile")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin.master@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin.master@example.com"));
    }
}
