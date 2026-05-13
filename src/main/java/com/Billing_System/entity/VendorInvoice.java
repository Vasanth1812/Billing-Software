package com.Billing_System.entity;

import com.Billing_System.vendor.entity.Vendor;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents an invoice submitted by a vendor for goods delivered via a GRN.
 * Implements the 3-Way Match paradigm (PO vs GRN vs Invoice).
 */
@Entity
@Table(name = "vendor_invoices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "invoice_number", nullable = false, length = 50)
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grn_id", nullable = false)
    private GRN grn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    // Financial Totals
    @Column(name = "invoice_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal invoiceAmount; // Base amount without GST

    @Column(name = "gst_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal gstAmount; // Total GST claimed

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount; // Grand total (Base + GST)

    // 3-Way Matching Flags
    // MATCHED, MISMATCHED, PARTIAL, PENDING
    @Column(name = "po_match_status", nullable = false, length = 20)
    private String poMatchStatus; 

    @Column(name = "grn_match_status", nullable = false, length = 20)
    private String grnMatchStatus;

    @Column(name = "invoice_match_status", nullable = false, length = 20)
    private String invoiceMatchStatus;

    // TRUE only if all three above are MATCHED
    @Column(name = "three_way_match", nullable = false)
    private boolean threeWayMatch;

    // Gov E-Invoicing Reference (India context)
    @Column(name = "irn_number", length = 64)
    private String irnNumber;

    // SUBMITTED, UNDER_REVIEW, APPROVED, REJECTED
    @Column(name = "submission_status", nullable = false, length = 20)
    private String submissionStatus;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
