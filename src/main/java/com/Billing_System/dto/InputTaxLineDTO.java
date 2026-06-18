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
    private UUID orderId;          // PurchaseOrder ID or GRN ID
    private String vendorName;     // Vendor legal name
    private String grnNumber;      // The actual GRN number
    private String batchNumber;    // The actual batch number (from VendorProduct)
    private String poNumber;       // PurchaseOrder invoiceNumber
    private LocalDate orderDate;   // PurchaseOrder invoiceDate
    private String productName;    // PurchaseItem productName
    private BigDecimal taxableAmount; // qty × purchaseRate (before GST)
    private BigDecimal gstRate;    // PurchaseItem gstRate (e.g. 5, 12, 18)
    private BigDecimal gstAmount;  // PurchaseItem gstAmount
}
