package com.Billing_System.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * A single row in the PO Status Report table.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class POReportRowDTO {

    private String id;
    private String invoiceNumber;
    private String vendorName;
    private String vendorCode;
    private String invoiceDate;
    private String dueDate;
    private String expectedDeliveryDate;
    private BigDecimal totalAmount;
    private BigDecimal gstAmount;
    private BigDecimal grandTotal;
    private String status;
    private String paymentMode;
    private String createdAt;
}
