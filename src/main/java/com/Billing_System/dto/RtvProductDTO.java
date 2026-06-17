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
public class RtvProductDTO {
    private UUID productId;
    private String productName;
    private String vendorSku;
    private String batchNumber;
    private BigDecimal returnedQuantity;
    private BigDecimal unitPrice;
    private BigDecimal totalValue;
}
