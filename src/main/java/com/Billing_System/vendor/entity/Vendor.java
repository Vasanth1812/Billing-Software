package com.Billing_System.vendor.entity;

import jakarta.persistence.*;
import lombok.*;
import com.Billing_System.entity.User;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Vendor — supply-chain partner who supplies goods via Purchase Orders.
 *
 * Lifecycle:
 *   onboardingStage: CATEGORY_MANAGER_REVIEW → QUALITY_REVIEW → FINANCE_REVIEW → DIRECTOR_REVIEW → null (ACTIVE)
 *   kycStatus:       PENDING → IN_REVIEW → ACTIVE | REJECTED | BLOCKED
 *   complianceStatus: PENDING → COMPLIANT | EXPIRING_SOON | NON_COMPLIANT | BLOCKED
 *
 * NOTE: This is separate from the lightweight `Supplier` entity used for
 *       product traceability. Vendor tracks the full procurement lifecycle.
 */
@Entity
@Table(name = "vendors",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_vendor_gstin",         columnNames = "gstin"),
                @UniqueConstraint(name = "uk_vendor_primary_email", columnNames = "primary_email"),
                @UniqueConstraint(name = "uk_vendor_code",          columnNames = "vendor_code")
        },
        indexes = {
                @Index(name = "idx_vendor_kyc_status",         columnList = "kyc_status"),
                @Index(name = "idx_vendor_compliance_status",  columnList = "compliance_status"),
                @Index(name = "idx_vendor_deleted_at",         columnList = "deleted_at")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Human-readable ID: VND-000001, VND-000002, … Auto-generated in service */
    @Column(name = "vendor_code", nullable = false, length = 20)
    private String vendorCode;

    @Column(name = "legal_name", nullable = false, length = 120)
    private String legalName;

    @Column(name = "trade_name", length = 100)
    private String tradeName;

    /**
     * MANUFACTURER | DISTRIBUTOR | TRADER | IMPORTER | SERVICE_PROVIDER
     */
    @Column(name = "business_type", nullable = false, length = 30)
    private String businessType;

    /**
     * KYC workflow state.
     * PENDING → IN_REVIEW → ACTIVE | REJECTED | BLOCKED
     */
    @Builder.Default
    @Column(name = "kyc_status", nullable = false, length = 30)
    private String kycStatus = "PENDING";

    /**
     * Compliance state — driven by document expiry scan.
     * PENDING → COMPLIANT | EXPIRING_SOON | NON_COMPLIANT | BLOCKED
     */
    @Builder.Default
    @Column(name = "compliance_status", nullable = false, length = 30)
    private String complianceStatus = "PENDING";

    /**
     * Current onboarding approval stage.
     * NULL means the vendor has completed onboarding (is ACTIVE).
     * CATEGORY_MANAGER_REVIEW | QUALITY_REVIEW | FINANCE_REVIEW | DIRECTOR_REVIEW
     */
    @Column(name = "onboarding_stage", length = 40)
    private String onboardingStage;

    /** 15-char GSTIN */
    @Column(name = "gstin", length = 15)
    private String gstin;

    /** 10-char PAN */
    @Column(name = "pan_number", length = 10)
    private String panNumber;

    /**
     * REGULAR | COMPOSITION | SEZ | UNREGISTERED | EXPORT_ONLY
     */
    @Column(name = "gst_registration_type", length = 30)
    private String gstRegistrationType;

    /** LT_1CR | 1_10CR | 10_50CR | 50_200CR | GT_200CR */
    @Column(name = "annual_turnover_range", length = 20)
    private String annualTurnoverRange;

    @Column(name = "primary_mobile", nullable = false, length = 15)
    private String primaryMobile;

    @Column(name = "primary_email", nullable = false, length = 120)
    private String primaryEmail;

    @Column(name = "website", length = 255)
    private String website;

    /** Any notes about the vendor — onboarding comments, review notes, etc. */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /** FK to User who initiated onboarding */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    @ToString.Exclude
    private User createdBy;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    /** Soft delete — NULL = active record */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
