package com.Billing_System.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Flat DTO for the Output Tax (Sales GST) ledger.
 * Each row represents the GST collected on a single sale.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutputTaxLineDTO {
    private UUID saleId;            // SalesInvoice ID
    private String invoiceNumber;   // SalesInvoice invoiceNumber (e.g. INV-0042)
    private String category;        // Primary product category name
    private String vendorName;      // Derived from primary supplier of sold product
    private String batchNumber;     // Batch number of the product sold (if tracked)
    private String customerName;    // SalesInvoice customerName
    private LocalDate saleDate;     // SalesInvoice invoiceDate
    private BigDecimal taxableAmount; // SalesInvoice subtotal (before GST)
    private BigDecimal totalGstAmount; // cgst + sgst
    private BigDecimal grandTotal;  // SalesInvoice grandTotal
    private String paymentMode;     // SalesInvoice paymentMode
}
