package com.smartfinancialexpenseanalysis.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfinancialexpenseanalysis.dto.ChangePasswordRequest;
import com.smartfinancialexpenseanalysis.dto.UpdateProfileRequest;
import com.smartfinancialexpenseanalysis.entity.Role;
import com.smartfinancialexpenseanalysis.entity.User;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProfileControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User user;

    @BeforeEach
    void setUp() {
        user = userRepository.findByEmail("profileuser@example.com")
                .orElseGet(() -> userRepository.save(new User(
                        "Original Name",
                        "profileuser@example.com",
                        passwordEncoder.encode("CurrentPass@123"),
                        Role.USER,
                        true
                )));
    }

    @Test
    @DisplayName("Unauthenticated profile access returns 401")
    void unauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Get authenticated user profile details")
    @WithMockUser(username = "profileuser@example.com")
    void getProfileSuccess() throws Exception {
        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Original Name"))
                .andExpect(jsonPath("$.email").value("profileuser@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    @DisplayName("Update user profile name")
    @WithMockUser(username = "profileuser@example.com")
    void updateProfileNameSuccess() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest("Updated Name");

        mockMvc.perform(put("/api/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"));

        User updated = userRepository.findByEmail("profileuser@example.com").orElseThrow();
        assertEquals("Updated Name", updated.getName());
    }

    @Test
    @DisplayName("Change password with valid current password and confirmation")
    @WithMockUser(username = "profileuser@example.com")
    void changePasswordSuccess() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest(
                "CurrentPass@123",
                "NewSecretPass@456",
                "NewSecretPass@456"
        );

        mockMvc.perform(put("/api/profile/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        User updated = userRepository.findByEmail("profileuser@example.com").orElseThrow();
        assertTrue(passwordEncoder.matches("NewSecretPass@456", updated.getPassword()));
    }

    @Test
    @DisplayName("Change password with incorrect current password returns 400")
    @WithMockUser(username = "profileuser@example.com")
    void changePasswordIncorrectCurrentPasswordReturns400() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest(
                "WrongPassword",
                "NewSecretPass@456",
                "NewSecretPass@456"
        );

        mockMvc.perform(put("/api/profile/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Change password with mismatching confirmation returns 400")
    @WithMockUser(username = "profileuser@example.com")
    void changePasswordMismatchReturns400() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest(
                "CurrentPass@123",
                "NewSecretPass@456",
                "DifferentPass@789"
        );

        mockMvc.perform(put("/api/profile/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Change password with short password returns 400")
    @WithMockUser(username = "profileuser@example.com")
    void changePasswordShortPasswordReturns400() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest(
                "CurrentPass@123",
                "123",
                "123"
        );

        mockMvc.perform(put("/api/profile/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
