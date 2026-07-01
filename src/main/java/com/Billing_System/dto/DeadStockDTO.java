package com.Billing_System.dto;

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
public class DeadStockDTO {
    private UUID id;
    private String sku;
    private String name;
    private String category;
    private BigDecimal qty;
    private java.time.LocalDate lastSale;
    private long daysSinceLastSale;
    private BigDecimal value;
}
