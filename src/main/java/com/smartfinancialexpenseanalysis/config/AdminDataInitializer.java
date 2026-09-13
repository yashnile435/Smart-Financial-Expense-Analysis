package com.smartfinancialexpenseanalysis.config;

import com.smartfinancialexpenseanalysis.entity.Role;
import com.smartfinancialexpenseanalysis.entity.User;
import com.smartfinancialexpenseanalysis.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Seeds an initial administrator account upon application startup if configured
 * via environment variables (INITIAL_ADMIN_EMAIL and INITIAL_ADMIN_PASSWORD) and no
 * administrator account currently exists.
 * Avoids hardcoding predictable production credentials.
 */
@Component
public class AdminDataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminDataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.initial-admin.email:${INITIAL_ADMIN_EMAIL:}}")
    private String initialAdminEmail;

    @Value("${app.initial-admin.password:${INITIAL_ADMIN_PASSWORD:}}")
    private String initialAdminPassword;

    public AdminDataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!StringUtils.hasText(initialAdminEmail) || !StringUtils.hasText(initialAdminPassword)) {
            logger.info("No initial administrator credentials provided via environment configuration. Skipping admin bootstrap.");
            return;
        }

        if (userRepository.countByRole(Role.ADMIN) == 0) {
            String adminEmail = initialAdminEmail.trim().toLowerCase();
            if (!userRepository.existsByEmail(adminEmail)) {
                User admin = new User(
                        "System Administrator",
                        adminEmail,
                        passwordEncoder.encode(initialAdminPassword.trim()),
                        Role.ADMIN,
                        true
                );
                userRepository.save(admin);
                logger.info("Initialized administrator account for: {}", adminEmail);
            }
        }
    }
}
