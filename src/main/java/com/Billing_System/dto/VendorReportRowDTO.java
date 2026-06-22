package com.Billing_System.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single row in the Vendor Master Report table.
 * Maps vendor entity fields to the columns displayed in the Reports Hub frontend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorReportRowDTO {

    private String id;
    private String vendorCode;
    private String name;
    private String category;       // businessType
    private String status;         // kycStatus
    private String complianceStatus;
    private String gstin;
    private String mobile;
    private String email;
    private Double rating;         // computed from scorecard (nullable)
    private String tier;
    private String city;           // from primary location
    private String onboardedDate;
}
