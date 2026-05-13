package com.Billing_System.vendor.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Response DTO for a single vendor product */
@Data
@Builder
public class VendorProductDTO {
    private UUID       id;
    private String     vendorCode;
    private String     vendorLegalName;
    private String     productName;
    private String     vendorSku;
    private BigDecimal purchasePrice;
    private String     unitOfMeasure;
    private String     packSize;
    private BigDecimal gstRate;
    private String     hsnCode;
    private String     brand;
    private String     category;
    private BigDecimal minOrderQty;
    private String     description;
    private UUID       mappedProductId;  // store product link
    private boolean    isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
