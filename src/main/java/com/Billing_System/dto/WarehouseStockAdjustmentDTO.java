package com.Billing_System.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class WarehouseStockAdjustmentDTO {
    @NotNull(message = "Product ID is required")
    private UUID productId;

    @NotBlank(message = "Rack ID is required")
    private String rackId;

    @NotBlank(message = "Type (IN or OUT) is required")
    private String type;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}
