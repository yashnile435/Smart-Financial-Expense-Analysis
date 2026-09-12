package com.smartfinancialexpenseanalysis.service;

import com.smartfinancialexpenseanalysis.dto.ApiResponse;
import com.smartfinancialexpenseanalysis.dto.AuthResponse;
import com.smartfinancialexpenseanalysis.dto.LoginRequest;
import com.smartfinancialexpenseanalysis.dto.RegisterRequest;
import com.smartfinancialexpenseanalysis.dto.UserResponse;
import com.smartfinancialexpenseanalysis.entity.Role;
import com.smartfinancialexpenseanalysis.entity.User;
import com.smartfinancialexpenseanalysis.exception.DuplicateResourceException;
import com.smartfinancialexpenseanalysis.exception.UnauthorizedException;
import com.smartfinancialexpenseanalysis.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

/**
 * Service managing user registration, authentication, sessions, and profile retrieval.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       SecurityContextRepository securityContextRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    /**
     * Registers a new user with standard USER role and BCrypt-hashed password.
     *
     * @param request registration details
     * @return AuthResponse containing registered user details without password
     */
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException("An account with this email already exists");
        }

        // Hash plain text password using BCrypt
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        // All self-registered users strictly receive Role.USER
        User newUser = new User(
                request.getName().trim(),
                normalizedEmail,
                hashedPassword,
                Role.USER
        );

        User savedUser = userRepository.save(newUser);

        UserResponse userResponse = new UserResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole()
        );

        return new AuthResponse(true, "User registered successfully", userResponse);
    }

    /**
     * Authenticates a user and establishes an HTTP session.
     *
     * @param request      login credentials
     * @param httpRequest  current HTTP request to attach session
     * @param httpResponse current HTTP response
     * @return AuthResponse containing authenticated user details
     */
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
            );
        } catch (AuthenticationException ex) {
            // Generic message prevents account enumeration
            throw new BadCredentialsException("Invalid email or password");
        }

        // Store authentication into the Spring SecurityContext and session
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        UserResponse userResponse = new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );

        return new AuthResponse(true, "Login successful", userResponse);
    }

    /**
     * Retrieves the profile of the currently authenticated user.
     *
     * @param authentication current security context authentication
     * @return UserResponse without sensitive fields
     */
    public UserResponse getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new UnauthorizedException("User is not authenticated");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }

    /**
     * Logs out the user by clearing the security context and invalidating the session.
     *
     * @param httpRequest current HTTP request
     * @return ApiResponse confirming logout
     */
    public ApiResponse logout(HttpServletRequest httpRequest) {
        SecurityContextHolder.clearContext();
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return new ApiResponse(true, "Logged out successfully");
    }
}
