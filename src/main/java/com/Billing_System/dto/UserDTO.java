package com.Billing_System.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UserDTO {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    /**
     * Role must be one of: ADMIN, MANAGER, CASHIER
     */
    @NotBlank(message = "Role is required")
    @Pattern(regexp = "ADMIN|MANAGER|CASHIER", message = "Role must be ADMIN, MANAGER, or CASHIER")
    private String role;
}
