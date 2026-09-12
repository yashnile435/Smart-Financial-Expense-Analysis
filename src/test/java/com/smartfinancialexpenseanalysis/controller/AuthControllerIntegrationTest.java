package com.smartfinancialexpenseanalysis.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfinancialexpenseanalysis.dto.LoginRequest;
import com.smartfinancialexpenseanalysis.dto.RegisterRequest;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for Stage 3 Authentication & Role-Based Authorization.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private final String testEmail = "testuser" + System.currentTimeMillis() + "@example.com";
    private final String testPassword = "securePassword123";

    @BeforeEach
    void setUp() {
        // Pre-create an admin and standard user if needed
    }

    @Test
    @DisplayName("POST /api/auth/register - Success with BCrypt hashing and USER role")
    void testRegisterSuccess() throws Exception {
        RegisterRequest request = new RegisterRequest("Test User", testEmail, testPassword);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.user.name").value("Test User"))
                .andExpect(jsonPath("$.user.email").value(testEmail))
                .andExpect(jsonPath("$.user.role").value("USER"))
                .andExpect(jsonPath("$.user.password").doesNotExist());

        // Verify database persistence and password hashing
        User saved = userRepository.findByEmail(testEmail).orElse(null);
        assertNotNull(saved);
        assertNotEquals(testPassword, saved.getPassword());
        assertTrue(passwordEncoder.matches(testPassword, saved.getPassword()));
        assertEquals(Role.USER, saved.getRole());
    }

    @Test
    @DisplayName("POST /api/auth/register - Rejects duplicate email")
    void testRegisterDuplicateEmail() throws Exception {
        RegisterRequest request = new RegisterRequest("User One", testEmail, testPassword);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Attempt to register with the exact same email
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("An account with this email already exists"));
    }

    @Test
    @DisplayName("POST /api/auth/register - Rejects short password (< 6 chars)")
    void testRegisterShortPassword() throws Exception {
        RegisterRequest request = new RegisterRequest("User Bad", "badpass@example.com", "123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/auth/login - Success establishes session, then GET /api/auth/me works")
    void testLoginSuccessAndSessionMe() throws Exception {
        // First register user
        RegisterRequest registerReq = new RegisterRequest("Login Tester", testEmail, testPassword);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        // Attempt login
        LoginRequest loginReq = new LoginRequest(testEmail, testPassword);
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.user.email").value(testEmail))
                .andExpect(jsonPath("$.user.role").value("USER"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(session, "Session must be created upon successful login");

        // Verify /api/auth/me with session
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(testEmail))
                .andExpect(jsonPath("$.name").value("Login Tester"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @DisplayName("POST /api/auth/login - Rejects incorrect password")
    void testLoginInvalidPassword() throws Exception {
        RegisterRequest registerReq = new RegisterRequest("Login Tester", testEmail, testPassword);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        LoginRequest wrongLoginReq = new LoginRequest(testEmail, "wrongSecret");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongLoginReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("GET /api/auth/me - Returns 401 when unauthenticated")
    void testGetMeUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/auth/logout - Invalidates session")
    void testLogout() throws Exception {
        RegisterRequest registerReq = new RegisterRequest("Logout Tester", testEmail, testPassword);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        LoginRequest loginReq = new LoginRequest(testEmail, testPassword);
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        // Perform logout
        mockMvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        assertTrue(session.isInvalid(), "Session must be invalidated after logout");
    }

    @Test
    @DisplayName("GET /api/admin/test - Returns 401 for unauthenticated request")
    void testAdminEndpointUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/admin/test"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "standard@example.com", roles = {"USER"})
    @DisplayName("GET /api/admin/test - Returns 403 Forbidden for USER role")
    void testAdminEndpointForbiddenForUser() throws Exception {
        mockMvc.perform(get("/api/admin/test"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    @DisplayName("GET /api/admin/test - Returns 200 OK for ADMIN role")
    void testAdminEndpointSuccessForAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Welcome Admin! Role-based access verified."));
    }
}
