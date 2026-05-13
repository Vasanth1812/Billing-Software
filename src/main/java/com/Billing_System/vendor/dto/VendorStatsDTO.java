package com.Billing_System.vendor.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Response DTO for GET /api/vendors/{id}/stats
 * Quick dashboard numbers for a vendor.
 */
@Data
@Builder
public class VendorStatsDTO {
    private String     vendorCode;
    private String     legalName;
    private int        totalPurchaseOrders;
    private BigDecimal totalSpend;          // sum of grandTotal across all POs
    private BigDecimal averageOrderValue;   // totalSpend / totalPOs
    private int        pendingOrders;       // status = "pending"
    private int        receivedOrders;      // status = "received"
    private int        cancelledOrders;     // status = "cancelled"
    private int        catalogProductCount; // total products in vendor catalog
    private int        documentsTotal;
    private int        documentsApproved;
    private int        documentsPending;
    private int        documentsExpired;    // approved but past expiry
    private String     complianceStatus;
    private String     kycStatus;
}
