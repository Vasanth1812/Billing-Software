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
public class RtvItemRequestDTO {
    private UUID productId;
    private UUID vendorProductId;
    private String productName;
    private String productSku;
    private String vendorProductSku;
    private BigDecimal quantity;
    private BigDecimal returnQuantity;
    private BigDecimal unitPrice;
    private String reason;
    private String returnReason;
}
