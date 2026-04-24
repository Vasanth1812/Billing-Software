package com.Billing_System.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class PurchaseRequestDTO {

    @NotNull(message = "Supplier ID is required")
    private String supplierId;

    private String invoiceNumber;

    private LocalDate invoiceDate;

    @NotBlank(message = "Payment mode is required")
    private String paymentMode;

    private LocalDate dueDate;

    private String status;

    @NotEmpty(message = "Purchase must have at least one item")
    @Valid
    private List<PurchaseItemDTO> items;

    @Data
    public static class PurchaseItemDTO {

        @NotNull(message = "Product ID is required")
        private String productId;

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.001", message = "Quantity must be greater than 0")
        private BigDecimal quantity;

        @NotNull(message = "Purchase rate is required")
        @DecimalMin(value = "0.0", message = "Purchase rate cannot be negative")
        private BigDecimal purchaseRate;

        @DecimalMin(value = "0.0", message = "GST rate cannot be negative")
        @DecimalMax(value = "100.0", message = "GST rate cannot exceed 100%")
        private BigDecimal gstRate;

        @DecimalMin(value = "0.0", message = "Discount cannot be negative")
        @DecimalMax(value = "100.0", message = "Discount cannot exceed 100%")
        private BigDecimal discountPct; // e.g. 5.0 = 5% discount off purchase rate
    }
}
