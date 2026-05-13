package com.Billing_System.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class GRNItemRequestDTO {

    @NotNull(message = "Purchase Item ID is required")
    private UUID purchaseItemId;

    @NotNull(message = "Product ID is required")
    private UUID productId;

    // Optional: Only used if vendor items are strictly tracked
    private UUID vendorProductId;

    private BigDecimal orderedQuantity;
    private BigDecimal receivedQuantity;
    private BigDecimal acceptedQuantity;
    private BigDecimal rejectedQuantity;

    private BigDecimal unitPrice;
    private String remarks;
}
