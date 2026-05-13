package com.Billing_System.entity;

import com.Billing_System.vendor.entity.Vendor;
import com.Billing_System.vendor.entity.VendorBankAccount;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents an outbound payment made to a vendor.
 * Includes deductions for Shortages (hold_amount) and GST mismatches (itc_hold_amount).
 */
@Entity
@Table(name = "vendor_payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "payment_number", nullable = false, unique = true, length = 20)
    private String paymentNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private VendorInvoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    // FULL, PARTIAL, ADVANCE, ON_ACCOUNT
    @Column(name = "payment_mode", nullable = false, length = 20)
    private String paymentMode;

    @Column(name = "payment_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal paymentAmount; // The gross amount intended to pay

    @Column(name = "hold_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal holdAmount; // Deductions for damages/shortages (From ShortageReport)

    @Column(name = "itc_hold_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal itcHoldAmount; // Deductions for GST mismatch

    @Column(name = "net_payment", nullable = false, precision = 15, scale = 2)
    private BigDecimal netPayment; // The actual amount that leaves our bank account

    @Column(name = "hold_reason", length = 500)
    private String holdReason;

    @Column(name = "payment_due_date", nullable = false)
    private LocalDate paymentDueDate;

    @Column(name = "early_pay_discount_pct", precision = 5, scale = 2)
    private BigDecimal earlyPayDiscountPct;

    @Column(name = "early_pay_discount_value", precision = 12, scale = 2)
    private BigDecimal earlyPayDiscountValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id", nullable = false)
    private VendorBankAccount bankAccount;

    // PENDING, APPROVED, PROCESSED, FAILED, CANCELLED
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "bank_reference", length = 50)
    private String bankReference; // e.g. UTR Number after successful transfer

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
