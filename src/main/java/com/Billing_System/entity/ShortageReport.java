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
 * Shortage Report (Debit Note Generator).
 * Automatically created when a GRN is finalized with rejected/missing quantities.
 */
@Entity
@Table(name = "shortage_reports",
        indexes = {
            @Index(name = "idx_sr_grn_id",    columnList = "grn_id"),
            @Index(name = "idx_sr_vendor_id",  columnList = "vendor_id"),
            @Index(name = "idx_sr_status",     columnList = "status")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShortageReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grn_id", nullable = false)
    private GRN grn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Column(name = "report_number", nullable = false, unique = true, length = 20)
    private String reportNumber;

    // Total financial value of the missing/rejected goods
    @Column(name = "total_shortage_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalShortageValue;

    // OPEN, VENDOR_NOTIFIED, DEBIT_RAISED, RESOLVED, WAIVED
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
