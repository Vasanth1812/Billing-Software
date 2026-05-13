package com.Billing_System.vendor.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/** Lightweight vendor summary — used for GET /api/vendors list view */
@Data
@Builder
public class VendorSummaryDTO {
    private UUID   id;
    private String vendorCode;
    private String legalName;
    private String tradeName;
    private String businessType;
    private String kycStatus;
    private String complianceStatus;
    private String onboardingStage;
    private String gstin;
    private String primaryMobile;
    private String primaryEmail;
    private LocalDateTime createdAt;
    // nearest expiring document info (for dashboard warning badges)
    private String nearestExpiryDate;
    private int    docsExpiringSoon;  // docs expiring within 30 days
    private int    docsExpired;       // already expired approved docs
}
