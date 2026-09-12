package com.smartfinancialexpenseanalysis.controller;

import com.smartfinancialexpenseanalysis.dto.ApiResponse;
import com.smartfinancialexpenseanalysis.dto.AuthResponse;
import com.smartfinancialexpenseanalysis.dto.LoginRequest;
import com.smartfinancialexpenseanalysis.dto.RegisterRequest;
import com.smartfinancialexpenseanalysis.dto.UserResponse;
import com.smartfinancialexpenseanalysis.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for user registration, authentication, sessions, and profile retrieval.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Registers a new user.
     *
     * @param request registration details
     * @return 201 Created with safe user profile
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Authenticates user and creates an HTTP session.
     *
     * @param request      login credentials
     * @param httpRequest  HTTP request for session association
     * @param httpResponse HTTP response
     * @return 200 OK with authenticated user profile
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest,
                                              HttpServletResponse httpResponse) {
        AuthResponse response = authService.login(request, httpRequest, httpResponse);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the profile of the currently logged-in user.
     *
     * @param authentication current security context authentication
     * @return 200 OK with user profile or 401 if unauthenticated
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        UserResponse response = authService.getCurrentUser(authentication);
        return ResponseEntity.ok(response);
    }

    /**
     * Logs out the user and destroys the authenticated session.
     *
     * @param httpRequest current HTTP request
     * @return 200 OK confirmation message
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(HttpServletRequest httpRequest) {
        ApiResponse response = authService.logout(httpRequest);
        return ResponseEntity.ok(response);
    }
}
