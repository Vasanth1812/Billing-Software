package com.Billing_System.vendor.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for GET /api/vendors/{id}/purchase-orders
 * Shows all POs raised against this vendor.
 */
@Data
@Builder
public class VendorPurchaseHistoryDTO {

    private UUID       purchaseOrderId;
    private String     invoiceNumber;
    private LocalDate  invoiceDate;
    private BigDecimal totalAmount;
    private BigDecimal gstAmount;
    private BigDecimal grandTotal;
    private String     paymentMode;
    private LocalDate  dueDate;
    private String     status;          // pending | received | cancelled
    private int        itemCount;       // number of line items
    private LocalDateTime createdAt;
}
