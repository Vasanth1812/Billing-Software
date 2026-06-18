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
public class SalesGstReportDTO {
    private String productName;
    private String categoryName;
    private String vendorName;
    private BigDecimal totalQuantitySold;
    private BigDecimal totalSalesValue;
    private BigDecimal totalGstCollected;
}
