package com.Billing_System.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Login request — supports TWO ways:
 * Way 1: email + password    → set email field
 * Way 2: userId + password   → set userId field (e.g. "EMP001")
 *
 * At least one of email or userId must be provided.
 */
@Data
public class LoginRequestDTO {

    /** Login option 1 — email address */
    private String email;

    /** Login option 2 — userId like "EMP001" */
    private String userId;

    @NotBlank(message = "Password is required")
    private String password;
}
