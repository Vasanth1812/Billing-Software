package com.Billing_System.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Login response — returned after successful authentication.
 */
@Data
@Builder
public class LoginResponseDTO {
    private String token;          // JWT token
    private String tokenType;      // "Bearer"
    private UUID id;               // user UUID
    private String userId;         // human-readable ID like "EMP001"
    private String name;
    private String email;
    private String role;           // ADMIN / MANAGER / CASHIER
}
