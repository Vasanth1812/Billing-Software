package com.Billing_System.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GstSummaryDTO {
    private BigDecimal outputGstSales;
    private int salesInvoiceCount;
    private BigDecimal inputGstPurchases;
    private int purchaseOrderCount;
    private BigDecimal netGstPayable;
    private BigDecimal itcAvailable;
    private List<GstSlabSummaryDTO> slabs;
}
