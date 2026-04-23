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
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Seed Admin User
        if (userRepository.count() == 0) {
            User admin = User.builder()
                    .userId("ADMIN")
                    .name("System Admin")
                    .email("sriniwebdesigner1998@gmail.com")
                    .password(passwordEncoder.encode("admin123"))
                    .role("ADMIN")
                    .build();
            userRepository.save(admin);
            log.info("═══════════════════════════════════════════════");
            log.info("  DEFAULT ADMIN CREATED");
            log.info("  User ID  : ADMIN");
            log.info("  Email    : sriniwebdesigner1998@gmail.com");
            log.info("  Password : admin123");
            log.info("  ⚠️  CHANGE THIS PASSWORD AFTER FIRST LOGIN!");
            log.info("═══════════════════════════════════════════════");
        }
    }
}
