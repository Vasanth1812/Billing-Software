package com.Billing_System.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Forgot password step 1 — user sends their email to receive a reset link/token.
 */
@Data
public class ForgotPasswordDTO {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
}
