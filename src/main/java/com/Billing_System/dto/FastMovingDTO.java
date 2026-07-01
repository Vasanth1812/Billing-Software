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
public class FastMovingDTO {
    private UUID id;
    private int rank;
    private String sku;
    private String name;
    private String category;
    private BigDecimal unitsSold;
    private BigDecimal revenue;
    private BigDecimal avgDailySales;

    // JPQL Constructor
    public FastMovingDTO(UUID id, String sku, String name, String category, BigDecimal unitsSold, BigDecimal revenue) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.unitsSold = unitsSold;
        this.revenue = revenue;
    }
}
