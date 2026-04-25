package com.Billing_System.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GstSlabSummaryDTO {
    private String gstSlab; // e.g., "5%", "12%"
    private BigDecimal outputTaxable;
    private BigDecimal cgst;
    private BigDecimal sgst;
    private BigDecimal totalOutputGst;
    private BigDecimal inputTaxable;
    private BigDecimal cgstItc;
    private BigDecimal sgstItc;
    private BigDecimal totalInputGst;
    private BigDecimal netPayable;
}
