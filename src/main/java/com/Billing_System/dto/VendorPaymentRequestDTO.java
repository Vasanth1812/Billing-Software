package com.Billing_System.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class VendorPaymentRequestDTO {
    @NotNull(message = "Invoice ID is required")
    private UUID invoiceId;

    @NotNull(message = "Vendor ID is required")
    private UUID vendorId;

    @NotBlank(message = "Payment mode is required")
    private String paymentMode; // FULL, PARTIAL

    @NotNull(message = "Payment amount is required")
    private BigDecimal paymentAmount; // Amount requested to be paid

    @NotNull(message = "Bank account ID is required")
    private UUID bankAccountId;

    private LocalDate paymentDueDate; // Optional override
}
