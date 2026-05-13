package com.Billing_System.entity;

import com.Billing_System.vendor.entity.Vendor;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Return To Vendor (RTV) Request.
 * Manages the physical return of damaged/rejected goods back to the supplier.
 */
@Entity
@Table(name = "rtv_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RtvRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "rtv_number", nullable = false, unique = true, length = 20)
    private String rtvNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grn_id", nullable = false)
    private GRN grn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    // FLAGGED, DEBIT_NOTE_RAISED, VENDOR_NOTIFIED, SHIPPED_BACK, RESOLVED, DISPUTED
    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "total_return_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalReturnValue;

    // Link to the financial Debit Note / Shortage Report
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shortage_report_id")
    private ShortageReport shortageReport;

    @Column(name = "dispute_note", length = 500)
    private String disputeNote;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
