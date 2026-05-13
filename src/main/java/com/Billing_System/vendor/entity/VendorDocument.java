package com.Billing_System.vendor.entity;

import jakarta.persistence.*;
import lombok.*;
import com.Billing_System.entity.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Compliance document uploaded by a vendor during onboarding or renewal.
 *
 * The compliance scheduler scans expiry_date daily and blocks vendors
 * with expired APPROVED documents.
 */
@Entity
@Table(name = "vendor_documents",
        indexes = {
                @Index(name = "idx_vendor_doc_vendor",       columnList = "vendor_id"),
                @Index(name = "idx_vendor_doc_expiry",       columnList = "expiry_date"),
                @Index(name = "idx_vendor_doc_status",       columnList = "upload_status")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    @ToString.Exclude
    private Vendor vendor;

    /**
     * GSTIN | FSSAI | PAN | TRADE_LICENSE | DRUG_LICENSE | CIN | OTHER
     */
    @Column(name = "doc_type", nullable = false, length = 30)
    private String docType;

    @Column(name = "doc_number", nullable = false, length = 50)
    private String docNumber;

    /**
     * NULL for non-expiring docs (PAN).
     * Required for FSSAI, Trade License, Drug License.
     * Compliance scheduler watches this column.
     */
    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    /**
     * File path / URL stored after upload.
     * In production → S3/MinIO object key.
     * For now → relative file path stored as string.
     */
    @Column(name = "file_reference", length = 500)
    private String fileReference;

    /** PENDING | VIRUS_SCAN | APPROVED | REJECTED */
    @Builder.Default
    @Column(name = "upload_status", nullable = false, length = 20)
    private String uploadStatus = "PENDING";

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    /** Staff member who approved the document */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    @ToString.Exclude
    private User verifiedBy;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
