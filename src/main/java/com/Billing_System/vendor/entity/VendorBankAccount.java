package com.Billing_System.vendor.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Bank account for a vendor — used for payment disbursement.
 * Account number hash (SHA-256) enables duplicate detection without storing plaintext.
 */
@Entity
@Table(name = "vendor_bank_accounts",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_vendor_bank_account_hash", columnNames = "account_number_hash")
        },
        indexes = {
                @Index(name = "idx_vendor_bank_vendor", columnList = "vendor_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorBankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    @ToString.Exclude
    private Vendor vendor;

    @Column(name = "account_holder_name", nullable = false, length = 100)
    private String accountHolderName;

    @Column(name = "bank_name", nullable = false, length = 100)
    private String bankName;

    /** Stored as-is (in production, encrypt at rest) */
    @Column(name = "account_number", nullable = false, length = 18)
    private String accountNumber;

    /** SHA-256 hash of account number — used for fraud/duplicate detection */
    @Column(name = "account_number_hash", nullable = false, length = 64)
    private String accountNumberHash;

    /** 11-char IFSC code */
    @Column(name = "ifsc_code", nullable = false, length = 11)
    private String ifscCode;

    /** CURRENT | SAVINGS | CC | OD */
    @Column(name = "account_type", nullable = false, length = 10)
    private String accountType;

    @Builder.Default
    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = false;

    /** PENDING | VERIFIED | FAILED */
    @Builder.Default
    @Column(name = "verification_status", nullable = false, length = 20)
    private String verificationStatus = "PENDING";

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
