package com.Billing_System.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class StoRequestDTO {
    @NotNull(message = "Product ID is required")
    private UUID productId;

    @NotBlank(message = "Source branch is required")
    private String sourceBranchName;

    @NotBlank(message = "Destination branch is required")
    private String destBranchName;

    @NotNull(message = "Transfer quantity is required")
    private BigDecimal transferQuantity;

    @NotNull(message = "Transfer date is required")
    private LocalDate transferDate;

    @NotBlank(message = "Transfer mode is required")
    private String transferMode;

    @NotBlank(message = "Priority is required")
    private String priority;
    
    private BigDecimal capitalSaved;
}
