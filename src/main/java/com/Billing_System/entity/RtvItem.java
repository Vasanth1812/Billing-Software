package com.Billing_System.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a single item being returned to the vendor within an RTV Request.
 * This decouples the returned product from the GRN's initial receipt counts.
 */
@Entity
@Table(name = "rtv_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RtvItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rtv_request_id", nullable = false)
    @ToString.Exclude
    private RtvRequest rtvRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @ToString.Exclude
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_product_id")
    @ToString.Exclude
    private com.Billing_System.vendor.entity.VendorProduct vendorProduct;

    @Column(name = "returned_qty", nullable = false, precision = 10, scale = 3)
    private BigDecimal returnedQuantity;

    @Column(name = "unit_price", precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_value", precision = 15, scale = 2)
    private BigDecimal totalValue;

    @Column(name = "reason", length = 255)
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
