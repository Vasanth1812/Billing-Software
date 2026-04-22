package com.Billing_System.config;

import com.Billing_System.entity.User;
import com.Billing_System.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Runs on application startup.
 * Creates a default ADMIN user if no users exist in the database.
 * This solves the chicken-and-egg problem:
 *   - Can't login without a user
 *   - Can't create a user without logging in (JWT required)
 *   - So we auto-create the first admin on startup
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            User admin = User.builder()
                    .userId("ADMIN")
                    .name("System Admin")
                    .email("admin@billing.com")
                    .password(passwordEncoder.encode("admin123"))
                    .role("ADMIN")
                    .build();
            userRepository.save(admin);
            log.info("═══════════════════════════════════════════════");
            log.info("  DEFAULT ADMIN CREATED");
            log.info("  User ID  : ADMIN");
            log.info("  Email    : admin@billing.com");
            log.info("  Password : admin123");
            log.info("  ⚠️  CHANGE THIS PASSWORD AFTER FIRST LOGIN!");
            log.info("═══════════════════════════════════════════════");
        }
    }
}
