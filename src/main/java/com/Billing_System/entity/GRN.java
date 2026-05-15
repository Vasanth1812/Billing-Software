package com.Billing_System.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Goods Receipt Note (GRN) Header.
 * Officially records the receipt of inventory against a Purchase Order.
 */
@Entity
@Table(name = "grn_header")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GRN {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "grn_number", unique = true, nullable = false, length = 50)
    private String grnNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    @ToString.Exclude
    private PurchaseOrder purchaseOrder;

    // Vendor link mirrors the PO's vendor for reporting/analytics.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    @ToString.Exclude
    private com.Billing_System.vendor.entity.Vendor vendor;

    @Column(name = "received_date", nullable = false)
    private LocalDateTime receivedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "received_by_user_id", nullable = false)
    @ToString.Exclude
    private User receivedBy;

    // DRAFT, APPROVED (Stock updated), CANCELLED
    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private String status = "DRAFT";

    @Column(name = "vendor_invoice_number", length = 100)
    private String vendorInvoiceNumber;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "grn", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<GRNItem> items = new ArrayList<>();
}
