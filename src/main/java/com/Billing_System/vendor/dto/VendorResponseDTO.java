package com.Billing_System.vendor.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Full vendor response — used for GET /api/vendors/{id} */
@Data
@Builder
public class VendorResponseDTO {

    private UUID   id;
    private String vendorCode;
    private String legalName;
    private String tradeName;
    private String businessType;
    private String kycStatus;
    private String complianceStatus;
    private String onboardingStage;
    private boolean authRequired;
    private String gstin;
    private String panNumber;
    private String gstRegistrationType;
    private String annualTurnoverRange;
    private String primaryMobile;
    private String primaryEmail;
    private String website;
    private String notes;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Nested sub-resources
    private List<LocationDTO>    locations;
    private List<BankAccountDTO> bankAccounts;
    private List<DocumentDTO>    documents;

    // ─── Nested DTOs ───────────────────────────────────────────────────────────

    @Data
    @Builder
    public static class LocationDTO {
        private UUID    id;
        private String  locationType;
        private String  addressLine1;
        private String  addressLine2;
        private String  city;
        private String  stateCode;
        private String  pinCode;
        private Boolean isPrimary;
    }

    @Data
    @Builder
    public static class BankAccountDTO {
        private UUID    id;
        private String  accountHolderName;
        private String  bankName;
        private String  accountNumberMasked; // show only last 4 digits: ****4321
        private String  ifscCode;
        private String  accountType;
        private Boolean isPrimary;
        private String  verificationStatus;
    }

    @Data
    @Builder
    public static class DocumentDTO {
        private UUID   id;
        private String docType;
        private String docNumber;
        private String expiryDate;    // formatted as yyyy-MM-dd
        private String uploadStatus;
        private String rejectionReason;
        private String fileReference;
        private String verifiedByName;
        private String verifiedAt;
        private String createdAt;
        private int    daysToExpiry;  // negative = already expired
    }
}
