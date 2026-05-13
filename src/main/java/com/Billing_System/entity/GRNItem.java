package com.Billing_System.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Goods Receipt Note (GRN) Line Item.
 * Represents the actual physical quantities received vs ordered.
 */
@Entity
@Table(name = "grn_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GRNItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grn_id", nullable = false)
    @ToString.Exclude
    private GRN grn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_item_id", nullable = false)
    @ToString.Exclude
    private PurchaseItem purchaseItem;

    // The store's product that will receive the stock update.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @ToString.Exclude
    private Product product;

    // Optional link to Vendor's catalog SKU if applicable.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_product_id")
    @ToString.Exclude
    private com.Billing_System.vendor.entity.VendorProduct vendorProduct;

    @Column(name = "ordered_qty", nullable = false, precision = 10, scale = 3)
    private BigDecimal orderedQuantity;

    @Column(name = "received_qty", nullable = false, precision = 10, scale = 3)
    private BigDecimal receivedQuantity;

    @Column(name = "accepted_qty", nullable = false, precision = 10, scale = 3)
    private BigDecimal acceptedQuantity;

    @Column(name = "rejected_qty", nullable = false, precision = 10, scale = 3)
    private BigDecimal rejectedQuantity;

    // Cost verification at receipt
    @Column(name = "unit_price", precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "remarks", length = 255)
    private String remarks;
}
