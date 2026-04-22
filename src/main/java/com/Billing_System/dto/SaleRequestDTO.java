package com.Billing_System.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class SaleRequestDTO {

    private String customerName;

    private String customerPhone;

    private String customerGstin;

    @NotNull(message = "Invoice date is required")
    private LocalDate invoiceDate;

    @NotBlank(message = "Payment mode is required")
    private String paymentMode;

    private String status;

    @NotEmpty(message = "Sale must have at least one item")
    @Valid
    private List<SaleItemDTO> items;

    @Data
    public static class SaleItemDTO {

        @NotNull(message = "Product ID is required")
        private UUID productId;

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.001", message = "Quantity must be greater than 0")
        private BigDecimal quantity;

        @NotNull(message = "MRP is required")
        @DecimalMin(value = "0.0", message = "MRP cannot be negative")
        private BigDecimal mrp;

        @DecimalMin(value = "0.0", message = "Discount cannot be negative")
        @DecimalMax(value = "100.0", message = "Discount cannot exceed 100%")
        private BigDecimal discountPct;

        // @DecimalMin(value = "0.0", message = "GST rate cannot be negative")
        // @DecimalMax(value = "100.0", message = "GST rate cannot exceed 100%")
        // private BigDecimal gstRate;
    }
}
