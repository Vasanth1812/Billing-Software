package com.Billing_System.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class LogoutRequestDTO {

    @NotNull(message = "User ID is required")
    private UUID userId;
}
