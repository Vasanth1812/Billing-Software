package com.Billing_System.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Human-readable login ID like "EMP001", "CASHIER02".
     * Used for login option 2: userId + password.
     */
    @Column(name = "user_id", unique = true, nullable = false, length = 50)
    private String userId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @JsonIgnore   // Never expose password in API responses
    @Column(name = "password", nullable = false)
    private String password;

    /**
     * ADMIN / MANAGER / CASHIER
     */
    @Builder.Default
    @Column(name = "role", nullable = false, length = 20)
    private String role = "CASHIER";

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * Token used for forgot-password flow.
     * Set when user requests reset, cleared after successful reset.
     */
    @Column(name = "reset_token", length = 100)
    private String resetToken;

    @Column(name = "reset_token_expiry")
    private LocalDateTime resetTokenExpiry;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
