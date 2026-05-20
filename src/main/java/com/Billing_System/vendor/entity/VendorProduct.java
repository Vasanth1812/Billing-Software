package com.Billing_System.vendor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Vendor's own product catalog — separate from store's product table.
 *
 * Why separate?
 *   Vendor calls it: "AMU-MILK-500", purchase price ₹22, pack of 12
 *   Store calls it:  barcode "8901088001234", MRP ₹28, single unit
 *
 * These are the same item but different representations.
 * VendorProduct stores vendor's view. Product table stores store's view.
 * They are linked via mapped_product_id (optional, set during GRN).
 */
@Entity
@Table(name = "vendor_products",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_vendor_sku",
                columnNames = {"vendor_id", "vendor_sku"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Which vendor supplies this product */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    @ToString.Exclude
    private Vendor vendor;

    /** Vendor's own product name */
    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    /** Vendor's internal SKU / item code */
    @Column(name = "vendor_sku", nullable = false, length = 100)
    private String vendorSku;

    /** Purchase price (cost from vendor per unit) */
    @Column(name = "purchase_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal purchasePrice;

    /** Unit of measure: KG, PCS, LTR, BOX etc */
    @Column(name = "unit_of_measure", nullable = false, length = 20)
    private String unitOfMeasure;

    /** Optional: 500ml / 1kg / 12pcs — pack description */
    @Column(name = "pack_size", length = 50)
    private String packSize;

    /** GST rate: 0 / 5 / 12 / 18 / 28 */
    @Column(name = "gst_rate", precision = 5, scale = 2)
    private BigDecimal gstRate;

    /** HSN code for GST classification */
    @Column(name = "hsn_code", length = 20)
    private String hsnCode;

    /** Brand name (Amul, Nestle, etc.) */
    @Column(name = "brand", length = 100)
    private String brand;

    /** Category (Dairy, Beverages, Snacks etc.) */
    @Column(name = "category", length = 100)
    private String category;

    /** Minimum order quantity from vendor */
    @Column(name = "min_order_qty", precision = 10, scale = 3)
    private BigDecimal minOrderQty;

    /** Product description / notes */
    @Column(name = "description", length = 500)
    private String description;

    /** Batch number for the product */
    @Column(name = "batch_number", length = 100)
    private String batchNumber;

    /** Expiry date of the batch */
    @Column(name = "expiry_date")
    private java.time.LocalDate expiryDate;

    /**
     * Optional link to store's product table.
     * Set during GRN matching — maps vendor product to store product.
     * NULL until GRN is created and matched.
     */
    @Column(name = "mapped_product_id")
    private UUID mappedProductId;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
