package com.smartfinancialexpenseanalysis.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfinancialexpenseanalysis.dto.RecurringExpenseRequest;
import com.smartfinancialexpenseanalysis.entity.Category;
import com.smartfinancialexpenseanalysis.entity.PaymentMethod;
import com.smartfinancialexpenseanalysis.entity.RecurrenceFrequency;
import com.smartfinancialexpenseanalysis.entity.RecurringExpense;
import com.smartfinancialexpenseanalysis.entity.Role;
import com.smartfinancialexpenseanalysis.entity.User;
import com.smartfinancialexpenseanalysis.repository.CategoryRepository;
import com.smartfinancialexpenseanalysis.repository.ExpenseRepository;
import com.smartfinancialexpenseanalysis.repository.RecurringExpenseRepository;
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

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RecurringExpenseControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RecurringExpenseRepository recurringExpenseRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User userA;
    private User userB;
    private Category category;

    @BeforeEach
    void setUp() {
        recurringExpenseRepository.deleteAll();
        expenseRepository.deleteAll();

        userA = userRepository.findByEmail("userA@example.com")
                .orElseGet(() -> userRepository.save(new User("User A", "userA@example.com", "Password@123", Role.USER, true)));

        userB = userRepository.findByEmail("userB@example.com")
                .orElseGet(() -> userRepository.save(new User("User B", "userB@example.com", "Password@123", Role.USER, true)));

        category = categoryRepository.findByNameIgnoreCase("Bills")
                .orElseGet(() -> categoryRepository.save(new Category("Bills", "Monthly bills")));
    }

    @Test
    @DisplayName("Unauthenticated request to recurring expenses returns 401")
    void unauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/recurring-expenses"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Create recurring expense successfully")
    @WithMockUser(username = "userA@example.com")
    void createRecurringExpenseSuccess() throws Exception {
        RecurringExpenseRequest request = new RecurringExpenseRequest(
                category.getId(),
                new BigDecimal("1500.00"),
                "Broadband Internet",
                PaymentMethod.UPI,
                RecurrenceFrequency.MONTHLY,
                LocalDate.now(),
                LocalDate.now().plusYears(1)
        );

        mockMvc.perform(post("/api/recurring-expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(1500.0))
                .andExpect(jsonPath("$.description").value("Broadband Internet"))
                .andExpect(jsonPath("$.frequency").value("MONTHLY"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @DisplayName("Create recurring expense with endDate before startDate returns 400")
    @WithMockUser(username = "userA@example.com")
    void createRecurringExpenseInvalidDatesReturns400() throws Exception {
        RecurringExpenseRequest request = new RecurringExpenseRequest(
                category.getId(),
                new BigDecimal("500.00"),
                "Invalid Dates",
                PaymentMethod.UPI,
                RecurrenceFrequency.WEEKLY,
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(5)
        );

        mockMvc.perform(post("/api/recurring-expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("List own recurring expenses and enforce user isolation")
    @WithMockUser(username = "userA@example.com")
    void listOwnRecurringExpenses() throws Exception {
        RecurringExpense recA = new RecurringExpense(userA, category, new BigDecimal("1000.00"),
                "Netflix", PaymentMethod.CARD, RecurrenceFrequency.MONTHLY, LocalDate.now(), null, LocalDate.now(), true);
        recurringExpenseRepository.save(recA);

        RecurringExpense recB = new RecurringExpense(userB, category, new BigDecimal("2000.00"),
                "Gym", PaymentMethod.CARD, RecurrenceFrequency.MONTHLY, LocalDate.now(), null, LocalDate.now(), true);
        recurringExpenseRepository.save(recB);

        mockMvc.perform(get("/api/recurring-expenses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].description").value("Netflix"));
    }

    @Test
    @DisplayName("Toggle status changes active flag")
    @WithMockUser(username = "userA@example.com")
    void toggleStatusSuccess() throws Exception {
        RecurringExpense rec = new RecurringExpense(userA, category, new BigDecimal("1000.00"),
                "Subscription", PaymentMethod.UPI, RecurrenceFrequency.MONTHLY, LocalDate.now(), null, LocalDate.now(), true);
        rec = recurringExpenseRepository.save(rec);

        mockMvc.perform(put("/api/recurring-expenses/" + rec.getId() + "/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @DisplayName("Generate due recurring expense creates expense and advances next occurrence")
    @WithMockUser(username = "userA@example.com")
    void generateDueExpenseSuccess() throws Exception {
        LocalDate occurrenceDate = LocalDate.now().minusDays(1);
        RecurringExpense rec = new RecurringExpense(userA, category, new BigDecimal("2500.00"),
                "Electricity Bill", PaymentMethod.BANK_TRANSFER, RecurrenceFrequency.MONTHLY,
                occurrenceDate, null, occurrenceDate, true);
        rec = recurringExpenseRepository.save(rec);

        mockMvc.perform(post("/api/recurring-expenses/" + rec.getId() + "/generate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastGeneratedDate").value(occurrenceDate.toString()))
                .andExpect(jsonPath("$.nextOccurrence").value(occurrenceDate.plusMonths(1).toString()));

        // Verify expense was saved in database
        assertEquals(1, expenseRepository.findByUserId(userA.getId()).size());
    }

    @Test
    @DisplayName("Generate not due recurring expense returns 400")
    @WithMockUser(username = "userA@example.com")
    void generateNotDueExpenseReturns400() throws Exception {
        LocalDate futureDate = LocalDate.now().plusDays(5);
        RecurringExpense rec = new RecurringExpense(userA, category, new BigDecimal("2500.00"),
                "Electricity Bill", PaymentMethod.BANK_TRANSFER, RecurrenceFrequency.MONTHLY,
                futureDate, null, futureDate, true);
        rec = recurringExpenseRepository.save(rec);

        mockMvc.perform(post("/api/recurring-expenses/" + rec.getId() + "/generate"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Delete own recurring expense succeeds")
    @WithMockUser(username = "userA@example.com")
    void deleteRecurringExpenseSuccess() throws Exception {
        RecurringExpense rec = new RecurringExpense(userA, category, new BigDecimal("1000.00"),
                "Newspaper", PaymentMethod.CASH, RecurrenceFrequency.MONTHLY, LocalDate.now(), null, LocalDate.now(), true);
        rec = recurringExpenseRepository.save(rec);

        mockMvc.perform(delete("/api/recurring-expenses/" + rec.getId()))
                .andExpect(status().isOk());

        assertFalse(recurringExpenseRepository.findById(rec.getId()).isPresent());
    }
}
