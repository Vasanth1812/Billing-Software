package com.Billing_System.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Flat DTO for the Input Tax Credit ledger.
 * Each row represents the GST paid on a single purchase item from a vendor.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InputTaxLineDTO {
    private UUID orderId;          // PurchaseOrder ID
    private String vendorName;     // Vendor legal name
    private String batchNumber;    // GRN number (batch identifier) — may be null if no GRN yet
    private String poNumber;       // PurchaseOrder invoiceNumber
    private LocalDate orderDate;   // PurchaseOrder invoiceDate
    private String productName;    // PurchaseItem productName
    private BigDecimal taxableAmount; // qty × purchaseRate (before GST)
    private BigDecimal gstRate;    // PurchaseItem gstRate (e.g. 5, 12, 18)
    private BigDecimal gstAmount;  // PurchaseItem gstAmount
}
