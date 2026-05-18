package com.Billing_System.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class GRNItemResponseDTO {
    private UUID id;
    private UUID grnId;
    private UUID purchaseItemId;
    
    private UUID productId;
    private String productName;
    private String productBarcode;
    
    private UUID vendorProductId;
    private String vendorProductSku;
    
    private BigDecimal orderedQuantity;
    private BigDecimal receivedQuantity;
    private BigDecimal acceptedQuantity;
    private BigDecimal rejectedQuantity;
    
    private BigDecimal unitPrice;
    private BigDecimal gstRate;
    private String remarks;
}
