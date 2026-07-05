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
public class ProfitMarginDTO {
    private UUID id;
    private String sku;
    private String name;
    private BigDecimal units;
    private BigDecimal revenue;
    private BigDecimal cogs; // Cost of Goods Sold
    private BigDecimal profit;
    private BigDecimal margin;

    // JPQL Constructor
    public ProfitMarginDTO(UUID id, String sku, String name, BigDecimal units, BigDecimal revenue, BigDecimal cogs) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.units = units;
        this.revenue = revenue;
        this.cogs = cogs;
    }
}
