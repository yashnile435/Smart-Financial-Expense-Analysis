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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comprehensive integration tests for Stage 7 Reports & Financial Reporting Module.
 * Validates deterministic calculations, date bounds, exports, validations, and multi-user isolation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReportControllerIntegrationTest {

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
    private Category categoryUtilities;

    @BeforeEach
    void setUp() {
        userAlice = userRepository.findByEmail("alice.reports@example.com").orElseGet(() ->
                userRepository.save(new User("Alice Reports", "alice.reports@example.com", "passwordHash", Role.USER))
        );

        userBob = userRepository.findByEmail("bob.reports@example.com").orElseGet(() ->
                userRepository.save(new User("Bob Reports", "bob.reports@example.com", "passwordHash", Role.USER))
        );

        categoryFood = categoryRepository.findByName("Food").orElseGet(() ->
                categoryRepository.save(new Category("Food", "Food expenses"))
        );

        categoryTravel = categoryRepository.findByName("Travel").orElseGet(() ->
                categoryRepository.save(new Category("Travel", "Travel expenses"))
        );

        categoryUtilities = categoryRepository.findByName("Utilities").orElseGet(() ->
                categoryRepository.save(new Category("Utilities", "Utility bills"))
        );
    }

    // =========================================================================
    // 1 & 2: Authentication enforcement
    // =========================================================================

    @Test
    @WithMockUser(username = "alice.reports@example.com")
    @DisplayName("1. Authenticated user can generate monthly report (200 OK)")
    void testMonthlyReport_Authenticated_Success() throws Exception {
        mockMvc.perform(get("/api/reports/monthly")
                        .param("month", "9")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.reportTitle", containsString("SEPTEMBER 2026")))
                .andExpect(jsonPath("$.summary.totalExpenses").value(0.00))
                .andExpect(jsonPath("$.summary.expenseCount").value(0));
    }

    @Test
    @DisplayName("2. Unauthenticated monthly report returns 401 Unauthorized")
    void testMonthlyReport_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/reports/monthly")
                        .param("month", "9")
                        .param("year", "2026"))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // 3: Custom date-range report works
    // =========================================================================

    @Test
    @WithMockUser(username = "alice.reports@example.com")
    @DisplayName("3. Custom date-range report works")
    void testDateRangeReport_Authenticated_Success() throws Exception {
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("1200.00"),
                LocalDate.of(2026, 9, 5), PaymentMethod.UPI, "Groceries"));

        mockMvc.perform(get("/api/reports/date-range")
                        .param("startDate", "2026-09-01")
                        .param("endDate", "2026-09-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalExpenses").value(1200.00))
                .andExpect(jsonPath("$.summary.expenseCount").value(1))
                .andExpect(jsonPath("$.expenses", hasSize(1)));
    }

    // =========================================================================
    // 4, 5, 6: Validation tests
    // =========================================================================

    @Test
    @WithMockUser(username = "alice.reports@example.com")
    @DisplayName("4. Invalid month returns 400 Bad Request")
    void testMonthlyReport_InvalidMonth_Returns400() throws Exception {
        mockMvc.perform(get("/api/reports/monthly")
                        .param("month", "13")
                        .param("year", "2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Month must be between 1 and 12")));
    }

    @Test
    @WithMockUser(username = "alice.reports@example.com")
    @DisplayName("5. Invalid year returns 400 Bad Request")
    void testMonthlyReport_InvalidYear_Returns400() throws Exception {
        mockMvc.perform(get("/api/reports/monthly")
                        .param("month", "5")
                        .param("year", "1800"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Year must be between 1900 and 2100")));
    }

    @Test
    @WithMockUser(username = "alice.reports@example.com")
    @DisplayName("6. Start date after end date returns 400 Bad Request")
    void testDateRangeReport_StartDateAfterEndDate_Returns400() throws Exception {
        mockMvc.perform(get("/api/reports/date-range")
                        .param("startDate", "2026-09-20")
                        .param("endDate", "2026-09-10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Start date cannot be after end date")));
    }

    // =========================================================================
    // 7, 8, 9, 10, 11: Summary statistics calculations
    // =========================================================================

    @Test
    @WithMockUser(username = "alice.reports@example.com")
    @DisplayName("7-11. Total expenses, count, average, highest, lowest calculations")
    void testSummaryCalculations() throws Exception {
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("1000.00"), LocalDate.of(2026, 9, 2), PaymentMethod.CASH, "Meal"));
        expenseRepository.save(new Expense(userAlice, categoryTravel, new BigDecimal("2500.00"), LocalDate.of(2026, 9, 10), PaymentMethod.CARD, "Flight"));
        expenseRepository.save(new Expense(userAlice, categoryUtilities, new BigDecimal("500.00"), LocalDate.of(2026, 9, 15), PaymentMethod.UPI, "Electricity"));

        // Total = 4000.00, Count = 3, Avg = 4000 / 3 = 1333.33, Max = 2500.00, Min = 500.00
        mockMvc.perform(get("/api/reports/monthly")
                        .param("month", "9")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalExpenses").value(4000.00))
                .andExpect(jsonPath("$.summary.expenseCount").value(3))
                .andExpect(jsonPath("$.summary.averageExpense").value(1333.33))
                .andExpect(jsonPath("$.summary.highestExpense").value(2500.00))
                .andExpect(jsonPath("$.summary.lowestExpense").value(500.00));
    }

    // =========================================================================
    // 12 & 13: Category and Payment Method breakdowns
    // =========================================================================

    @Test
    @WithMockUser(username = "alice.reports@example.com")
    @DisplayName("12. Category breakdown calculations")
    void testCategoryBreakdown() throws Exception {
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("3000.00"), LocalDate.of(2026, 9, 2), PaymentMethod.UPI, "Dining"));
        expenseRepository.save(new Expense(userAlice, categoryTravel, new BigDecimal("1000.00"), LocalDate.of(2026, 9, 5), PaymentMethod.CARD, "Cab"));

        // Total = 4000.00 -> Food = 75.00%, Travel = 25.00%
        mockMvc.perform(get("/api/reports/monthly")
                        .param("month", "9")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryBreakdown", hasSize(2)))
                .andExpect(jsonPath("$.categoryBreakdown[0].categoryName").value("Food"))
                .andExpect(jsonPath("$.categoryBreakdown[0].totalAmount").value(3000.00))
                .andExpect(jsonPath("$.categoryBreakdown[0].percentage").value(75.00))
                .andExpect(jsonPath("$.categoryBreakdown[1].categoryName").value("Travel"))
                .andExpect(jsonPath("$.categoryBreakdown[1].percentage").value(25.00));
    }

    @Test
    @WithMockUser(username = "alice.reports@example.com")
    @DisplayName("13. Payment method breakdown calculations")
    void testPaymentMethodBreakdown() throws Exception {
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("800.00"), LocalDate.of(2026, 9, 2), PaymentMethod.UPI, "Snacks"));
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("200.00"), LocalDate.of(2026, 9, 3), PaymentMethod.CASH, "Coffee"));

        // Total = 1000.00 -> UPI = 80.00%, CASH = 20.00%
        mockMvc.perform(get("/api/reports/monthly")
                        .param("month", "9")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentMethodBreakdown", hasSize(2)))
                .andExpect(jsonPath("$.paymentMethodBreakdown[0].paymentMethod").value("UPI"))
                .andExpect(jsonPath("$.paymentMethodBreakdown[0].percentage").value(80.00))
                .andExpect(jsonPath("$.paymentMethodBreakdown[1].paymentMethod").value("CASH"))
                .andExpect(jsonPath("$.paymentMethodBreakdown[1].percentage").value(20.00));
    }

    // =========================================================================
    // 14, 15, 16, 17, 18: Date boundary inclusion and leap year
    // =========================================================================

    @Test
    @WithMockUser(username = "alice.reports@example.com")
    @DisplayName("14-17. First day and last day included, outside dates excluded")
    void testMonthlyReport_DateBoundaries() throws Exception {
        // September 2026: 2026-09-01 to 2026-09-30
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("100.00"), LocalDate.of(2026, 8, 31), PaymentMethod.CASH, "Before"));
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("200.00"), LocalDate.of(2026, 9, 1), PaymentMethod.CASH, "First Day"));
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("300.00"), LocalDate.of(2026, 9, 30), PaymentMethod.CASH, "Last Day"));
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("400.00"), LocalDate.of(2026, 10, 1), PaymentMethod.CASH, "After"));

        mockMvc.perform(get("/api/reports/monthly")
                        .param("month", "9")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalExpenses").value(500.00))
                .andExpect(jsonPath("$.summary.expenseCount").value(2));
    }

    @Test
    @WithMockUser(username = "alice.reports@example.com")
    @DisplayName("18. Leap year February correctly includes 29th day")
    void testLeapYearFebruary() throws Exception {
        // 2024 was a leap year
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("750.00"), LocalDate.of(2024, 2, 29), PaymentMethod.CARD, "Leap day"));

        mockMvc.perform(get("/api/reports/monthly")
                        .param("month", "2")
                        .param("year", "2024"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.startDate").value("2024-02-01"))
                .andExpect(jsonPath("$.summary.endDate").value("2024-02-29"))
                .andExpect(jsonPath("$.summary.totalExpenses").value(750.00))
                .andExpect(jsonPath("$.summary.expenseCount").value(1));
    }

    // =========================================================================
    // 19, 20, 21, 22, 23: Budget vs Actual calculations
    // =========================================================================

    @Test
    @WithMockUser(username = "alice.reports@example.com")
    @DisplayName("19-20. Budget vs Actual - UNDER_BUDGET (< 80%)")
    void testBudgetVsActual_UnderBudget() throws Exception {
        budgetRepository.save(new Budget(userAlice, 9, 2026, new BigDecimal("10000.00")));
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("5000.00"), LocalDate.of(2026, 9, 10), PaymentMethod.CARD, "Groceries"));

        mockMvc.perform(get("/api/reports/budget-vs-actual")
                        .param("month", "9")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budgetConfigured").value(true))
                .andExpect(jsonPath("$.budgetAmount").value(10000.00))
                .andExpect(jsonPath("$.actualExpense").value(5000.00))
                .andExpect(jsonPath("$.remainingAmount").value(5000.00))
                .andExpect(jsonPath("$.utilizationPercentage").value(50.00))
                .andExpect(jsonPath("$.status").value("UNDER_BUDGET"));
    }

    @Test
    @WithMockUser(username = "alice.reports@example.com")
    @DisplayName("21. Budget vs Actual - NEAR_LIMIT (80% - 100%)")
    void testBudgetVsActual_NearLimit() throws Exception {
        budgetRepository.save(new Budget(userAlice, 9, 2026, new BigDecimal("10000.00")));
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("8500.00"), LocalDate.of(2026, 9, 12), PaymentMethod.CARD, "Shopping"));

        mockMvc.perform(get("/api/reports/budget-vs-actual")
                        .param("month", "9")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.utilizationPercentage").value(85.00))
                .andExpect(jsonPath("$.status").value("NEAR_LIMIT"));
    }

    @Test
    @WithMockUser(username = "alice.reports@example.com")
    @DisplayName("22. Budget vs Actual - OVER_BUDGET (> 100%)")
    void testBudgetVsActual_OverBudget() throws Exception {
        budgetRepository.save(new Budget(userAlice, 9, 2026, new BigDecimal("10000.00")));
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("12000.00"), LocalDate.of(2026, 9, 15), PaymentMethod.CARD, "Overspend"));

        mockMvc.perform(get("/api/reports/budget-vs-actual")
                        .param("month", "9")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.utilizationPercentage").value(120.00))
                .andExpect(jsonPath("$.remainingAmount").value(-2000.00))
                .andExpect(jsonPath("$.status").value("OVER_BUDGET"));
    }

    @Test
    @WithMockUser(username = "alice.reports@example.com")
    @DisplayName("23. Budget vs Actual - No budget configured scenario")
    void testBudgetVsActual_NoBudgetConfigured() throws Exception {
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("1500.00"), LocalDate.of(2026, 9, 5), PaymentMethod.CASH, "Dinner"));

        mockMvc.perform(get("/api/reports/budget-vs-actual")
                        .param("month", "9")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budgetConfigured").value(false))
                .andExpect(jsonPath("$.actualExpense").value(1500.00))
                .andExpect(jsonPath("$.status").value("NOT_CONFIGURED"));
    }

    // =========================================================================
    // 24 & 25: Dedicated category & payment-method endpoints
    // =========================================================================

    @Test
    @WithMockUser(username = "alice.reports@example.com")
    @DisplayName("24. Category report endpoint returns list of category breakdown")
    void testCategoryReportEndpoint() throws Exception {
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("2000.00"), LocalDate.of(2026, 9, 5), PaymentMethod.CARD, "Food"));

        mockMvc.perform(get("/api/reports/category")
                        .param("startDate", "2026-09-01")
                        .param("endDate", "2026-09-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].categoryName").value("Food"))
                .andExpect(jsonPath("$[0].totalAmount").value(2000.00));
    }

    @Test
    @WithMockUser(username = "alice.reports@example.com")
    @DisplayName("25. Payment method report endpoint returns list of method breakdown")
    void testPaymentMethodReportEndpoint() throws Exception {
        expenseRepository.save(new Expense(userAlice, categoryTravel, new BigDecimal("3500.00"), LocalDate.of(2026, 9, 5), PaymentMethod.UPI, "Travel"));

        mockMvc.perform(get("/api/reports/payment-method")
                        .param("startDate", "2026-09-01")
                        .param("endDate", "2026-09-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].paymentMethod").value("UPI"))
                .andExpect(jsonPath("$[0].totalAmount").value(3500.00));
    }

    // =========================================================================
    // 26, 27, 28: CSV and PDF Exporting
    // =========================================================================

    @Test
    @WithMockUser(username = "alice.reports@example.com")
    @DisplayName("26. CSV export returns text/csv with attachment header and RFC 4180 format")
    void testCsvExport() throws Exception {
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("1500.00"),
                LocalDate.of(2026, 9, 10), PaymentMethod.CARD, "Dinner, with client"));

        mockMvc.perform(get("/api/reports/export/csv")
                        .param("month", "9")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv; charset=UTF-8"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment; filename=\"financial-report-2026-09.csv\"")))
                .andExpect(content().string(containsString("Date,Category,Description,Payment Method,Amount")))
                .andExpect(content().string(containsString("\"Dinner, with client\"")));
    }

    @Test
    @WithMockUser(username = "alice.reports@example.com")
    @DisplayName("27. PDF export returns application/pdf with PDF magic bytes (%PDF-)")
    void testPdfExport() throws Exception {
        expenseRepository.save(new Expense(userAlice, categoryFood, new BigDecimal("1500.00"),
                LocalDate.of(2026, 9, 10), PaymentMethod.CARD, "Team lunch"));

        byte[] pdfBytes = mockMvc.perform(get("/api/reports/export/pdf")
                        .param("month", "9")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment; filename=\"financial-report-2026-09.pdf\"")))
                .andReturn().getResponse().getContentAsByteArray();

        // Validate PDF magic bytes: starts with %PDF-
        assertTrue(pdfBytes.length > 100);
        String headerPrefix = new String(pdfBytes, 0, 5);
        assertTrue(headerPrefix.startsWith("%PDF-"));

        // Extract rendered text using OpenPDF PdfReader & PdfTextExtractor
        com.lowagie.text.pdf.PdfReader reader = new com.lowagie.text.pdf.PdfReader(pdfBytes);
        com.lowagie.text.pdf.parser.PdfTextExtractor extractor = new com.lowagie.text.pdf.parser.PdfTextExtractor(reader);
        StringBuilder extractedText = new StringBuilder();
        for (int i = 1; i <= reader.getNumberOfPages(); i++) {
            extractedText.append(extractor.getTextFromPage(i)).append("\n");
        }
        reader.close();
        String fullPdfText = extractedText.toString();

        // Verify that "Zero AI" is completely absent and the new clean footer is present
        org.junit.jupiter.api.Assertions.assertFalse(fullPdfText.contains("Zero AI"));
        org.junit.jupiter.api.Assertions.assertFalse(fullPdfText.toLowerCase().contains("zero ai"));
        org.junit.jupiter.api.Assertions.assertTrue(fullPdfText.contains("Generated by Smart Financial Expense Analysis. All calculations in INR."));
    }

    @Test
    @DisplayName("28. Exports require authentication (401 Unauthorized)")
    void testExports_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/reports/export/csv")
                        .param("month", "9")
                        .param("year", "2026"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/reports/export/pdf")
                        .param("month", "9")
                        .param("year", "2026"))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // 29 & 30: Multi-User Data Isolation (Section 24 requirement)
    // User A has ₹10,000, User B has ₹2,000
    // =========================================================================

    @Test
    @WithMockUser(username = "alice.reports@example.com")
    @DisplayName("29. User A sees only User A's report data (₹10,000)")
    void testUserDataIsolation_UserA() throws Exception {
        // Seed User A: ₹10,000
        expenseRepository.save(new Expense(userAlice, categoryTravel, new BigDecimal("10000.00"),
                LocalDate.of(2026, 9, 8), PaymentMethod.CARD, "User A flight"));

        // Seed User B: ₹2,000
        expenseRepository.save(new Expense(userBob, categoryFood, new BigDecimal("2000.00"),
                LocalDate.of(2026, 9, 8), PaymentMethod.UPI, "User B food"));

        mockMvc.perform(get("/api/reports/monthly")
                        .param("month", "9")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalExpenses").value(10000.00))
                .andExpect(jsonPath("$.summary.expenseCount").value(1))
                .andExpect(jsonPath("$.expenses[0].description").value("User A flight"));
    }

    @Test
    @WithMockUser(username = "bob.reports@example.com")
    @DisplayName("30. User B sees only User B's report data (₹2,000) and cannot see User A's data")
    void testUserDataIsolation_UserB() throws Exception {
        // Seed User A: ₹10,000
        expenseRepository.save(new Expense(userAlice, categoryTravel, new BigDecimal("10000.00"),
                LocalDate.of(2026, 9, 8), PaymentMethod.CARD, "User A flight"));

        // Seed User B: ₹2,000
        expenseRepository.save(new Expense(userBob, categoryFood, new BigDecimal("2000.00"),
                LocalDate.of(2026, 9, 8), PaymentMethod.UPI, "User B food"));

        mockMvc.perform(get("/api/reports/monthly")
                        .param("month", "9")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalExpenses").value(2000.00))
                .andExpect(jsonPath("$.summary.expenseCount").value(1))
                .andExpect(jsonPath("$.expenses[0].description").value("User B food"));
    }

    @Test
    @WithMockUser(username = "bob.reports@example.com")
    @DisplayName("31. User B cannot export User A's data via CSV")
    void testUserDataIsolation_CsvExport() throws Exception {
        expenseRepository.save(new Expense(userAlice, categoryTravel, new BigDecimal("10000.00"),
                LocalDate.of(2026, 9, 8), PaymentMethod.CARD, "Top Secret Flight of Alice"));
        expenseRepository.save(new Expense(userBob, categoryFood, new BigDecimal("2000.00"),
                LocalDate.of(2026, 9, 8), PaymentMethod.UPI, "Bob daily meal"));

        mockMvc.perform(get("/api/reports/export/csv")
                        .param("month", "9")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bob daily meal")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Top Secret Flight of Alice"))));
    }

    // =========================================================================
    // 32: Empty report returns valid zero values (not nulls)
    // =========================================================================

    @Test
    @WithMockUser(username = "alice.reports@example.com")
    @DisplayName("32. Empty report returns valid zero values (not nulls)")
    void testEmptyReport_ReturnsZeroValues() throws Exception {
        mockMvc.perform(get("/api/reports/date-range")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalExpenses").value(0.00))
                .andExpect(jsonPath("$.summary.expenseCount").value(0))
                .andExpect(jsonPath("$.summary.averageExpense").value(0.00))
                .andExpect(jsonPath("$.summary.highestExpense").value(0.00))
                .andExpect(jsonPath("$.summary.lowestExpense").value(0.00))
                .andExpect(jsonPath("$.categoryBreakdown", hasSize(0)))
                .andExpect(jsonPath("$.paymentMethodBreakdown", hasSize(0)))
                .andExpect(jsonPath("$.expenses", hasSize(0)));
    }
}
