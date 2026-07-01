package com.Billing_System.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMovementDTO {
    private UUID id;
    private String sku;
    private String name;
    
    @Builder.Default
    private BigDecimal opening = BigDecimal.ZERO;
    
    @Builder.Default
    private BigDecimal purchases = BigDecimal.ZERO;
    
    @Builder.Default
    private BigDecimal sales = BigDecimal.ZERO;
    
    @Builder.Default
    private BigDecimal returns = BigDecimal.ZERO;
    
    @Builder.Default
    private BigDecimal closing = BigDecimal.ZERO;
}
