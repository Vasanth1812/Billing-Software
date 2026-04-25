package com.Billing_System.dto;

import com.Billing_System.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryProductDTO {
    private UUID id;
    private String name;
    private String sku;
    private String categoryName;
    private String brand;
    private BigDecimal sellingPrice;
    private BigDecimal currentStock;
    private BigDecimal minStock;
    private String status; // OK, LOW, OUT
    private double level; // percentage (current/min * 100, capped at 100 or something)
}
