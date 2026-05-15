package com.Billing_System.vendor.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Request body for POST /api/vendors  (create new vendor)
 * and PUT /api/vendors/{id}           (update vendor)
 */
@Data
public class VendorRequestDTO {

    @NotBlank(message = "Legal name is required")
    @Size(max = 120)
    private String legalName;

    @Size(max = 100)
    private String tradeName;

    /**
     * MANUFACTURER | DISTRIBUTOR | TRADER | IMPORTER | SERVICE_PROVIDER
     */
    @NotBlank(message = "Business type is required")
    private String businessType;

    @Pattern(regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$",
             message = "Invalid GSTIN format (15 chars, e.g. 29ABCDE1234F1Z5)")
    private String gstin;

    @NotBlank(message = "PAN number is required")
    @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$",
             message = "Invalid PAN format (10 chars, e.g. ABCDE1234F)")
    private String panNumber;

    /**
     * REGULAR | COMPOSITION | SEZ | UNREGISTERED | EXPORT_ONLY
     */
    @NotBlank(message = "GST registration type is required")
    private String gstRegistrationType;

    @NotBlank(message = "Primary mobile is required")
    @Size(max = 15)
    private String primaryMobile;

    @NotBlank(message = "Primary email is required")
    @Email(message = "Invalid email format")
    private String primaryEmail;

    @Size(max = 255)
    private String website;

    /**
     * LT_1CR | 1_10CR | 10_50CR | 50_200CR | GT_200CR
     */
    private String annualTurnoverRange;

    private String notes;

    /**
     * Controls whether the 4-step onboarding review is required.
     * Default: false (0) — vendor is auto-activated without any approval workflow.
     * Set to true (1) if you want full CATEGORY_MANAGER → QUALITY → FINANCE → DIRECTOR review.
     */
    private boolean authRequired = false;
}
