package com.smartfinancialexpenseanalysis.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfinancialexpenseanalysis.dto.GoalContributionRequest;
import com.smartfinancialexpenseanalysis.dto.SavingsGoalRequest;
import com.smartfinancialexpenseanalysis.entity.Role;
import com.smartfinancialexpenseanalysis.entity.SavingsGoal;
import com.smartfinancialexpenseanalysis.entity.SavingsGoalStatus;
import com.smartfinancialexpenseanalysis.entity.User;
import com.smartfinancialexpenseanalysis.repository.SavingsGoalRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SavingsGoalControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SavingsGoalRepository savingsGoalRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        savingsGoalRepository.deleteAll();

        userA = userRepository.findByEmail("userA@example.com")
                .orElseGet(() -> userRepository.save(new User("User A", "userA@example.com", "Password@123", Role.USER, true)));

        userB = userRepository.findByEmail("userB@example.com")
                .orElseGet(() -> userRepository.save(new User("User B", "userB@example.com", "Password@123", Role.USER, true)));
    }

    @Test
    @DisplayName("Unauthenticated request to savings goals returns 401")
    void unauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/goals"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Create savings goal successfully")
    @WithMockUser(username = "userA@example.com")
    void createGoalSuccess() throws Exception {
        SavingsGoalRequest request = new SavingsGoalRequest(
                "New Laptop",
                "Saving for development laptop",
                new BigDecimal("80000.00"),
                new BigDecimal("20000.00"),
                LocalDate.now().plusMonths(6)
        );

        mockMvc.perform(post("/api/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Laptop"))
                .andExpect(jsonPath("$.targetAmount").value(80000.0))
                .andExpect(jsonPath("$.currentAmount").value(20000.0))
                .andExpect(jsonPath("$.remainingAmount").value(60000.0))
                .andExpect(jsonPath("$.progressPercentage").value(25.0))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("Create goal with invalid amount returns 400")
    @WithMockUser(username = "userA@example.com")
    void createGoalInvalidAmountReturns400() throws Exception {
        SavingsGoalRequest request = new SavingsGoalRequest(
                "Invalid Goal",
                "Zero target",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                LocalDate.now().plusMonths(1)
        );

        mockMvc.perform(post("/api/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("List own goals and verify user data isolation")
    @WithMockUser(username = "userA@example.com")
    void listOwnGoals() throws Exception {
        SavingsGoal goalA = new SavingsGoal(userA, "Goal A", "Desc A", new BigDecimal("10000.00"),
                new BigDecimal("2000.00"), LocalDate.now().plusMonths(3), SavingsGoalStatus.ACTIVE);
        savingsGoalRepository.save(goalA);

        SavingsGoal goalB = new SavingsGoal(userB, "Goal B", "Desc B", new BigDecimal("50000.00"),
                new BigDecimal("5000.00"), LocalDate.now().plusMonths(5), SavingsGoalStatus.ACTIVE);
        savingsGoalRepository.save(goalB);

        mockMvc.perform(get("/api/goals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Goal A"));
    }

    @Test
    @DisplayName("Get goal by ID strictly verifies ownership")
    @WithMockUser(username = "userA@example.com")
    void getGoalByIdOwnership() throws Exception {
        SavingsGoal goalB = new SavingsGoal(userB, "Goal B", "Desc B", new BigDecimal("50000.00"),
                new BigDecimal("5000.00"), LocalDate.now().plusMonths(5), SavingsGoalStatus.ACTIVE);
        savingsGoalRepository.save(goalB);

        mockMvc.perform(get("/api/goals/" + goalB.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Add contribution increases saved amount and computes remaining")
    @WithMockUser(username = "userA@example.com")
    void addContributionIncreasesBalance() throws Exception {
        SavingsGoal goal = new SavingsGoal(userA, "Emergency Fund", "Rainy day fund", new BigDecimal("100000.00"),
                new BigDecimal("30000.00"), LocalDate.now().plusYears(1), SavingsGoalStatus.ACTIVE);
        goal = savingsGoalRepository.save(goal);

        GoalContributionRequest request = new GoalContributionRequest(new BigDecimal("15000.00"));

        mockMvc.perform(post("/api/goals/" + goal.getId() + "/contributions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentAmount").value(45000.0))
                .andExpect(jsonPath("$.remainingAmount").value(55000.0))
                .andExpect(jsonPath("$.progressPercentage").value(45.0))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("Adding contribution transitions goal to COMPLETED when target reached")
    @WithMockUser(username = "userA@example.com")
    void addContributionCompletesGoal() throws Exception {
        SavingsGoal goal = new SavingsGoal(userA, "Watch", "Smart watch", new BigDecimal("20000.00"),
                new BigDecimal("18000.00"), LocalDate.now().plusMonths(1), SavingsGoalStatus.ACTIVE);
        goal = savingsGoalRepository.save(goal);

        GoalContributionRequest request = new GoalContributionRequest(new BigDecimal("2000.00"));

        mockMvc.perform(post("/api/goals/" + goal.getId() + "/contributions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentAmount").value(20000.0))
                .andExpect(jsonPath("$.remainingAmount").value(0.0))
                .andExpect(jsonPath("$.progressPercentage").value(100.0))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("Delete own savings goal succeeds")
    @WithMockUser(username = "userA@example.com")
    void deleteGoalSuccess() throws Exception {
        SavingsGoal goal = new SavingsGoal(userA, "Travel Fund", "Vacation", new BigDecimal("50000.00"),
                BigDecimal.ZERO, LocalDate.now().plusMonths(6), SavingsGoalStatus.ACTIVE);
        goal = savingsGoalRepository.save(goal);

        mockMvc.perform(delete("/api/goals/" + goal.getId()))
                .andExpect(status().isOk());

        assertFalse(savingsGoalRepository.findById(goal.getId()).isPresent());
    }
}
