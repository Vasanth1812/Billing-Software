package com.Billing_System.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "sku", nullable = false, unique = true, length = 50)
    private String sku;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private Category category;

    /**
     * Primary supplier for this product — used for traceability.
     * If a product causes harm, you immediately know which supplier to contact.
     * Nullable: existing products and walk-in purchases still work.
     * In bulk import (Option A): supplier name MUST exist in suppliers table.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "primary_supplier_id")
    private Supplier primarySupplier;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "purchase_rate", precision = 10, scale = 2)
    private BigDecimal purchaseRate;

    @Column(name = "mrp", precision = 10, scale = 2)
    private BigDecimal mrp;

    @Column(name = "gst_rate", precision = 5, scale = 2)
    private BigDecimal gstRate;

    @Column(name = "hsn_code", length = 20)
    private String hsnCode;

    @Column(name = "barcode", length = 50)
    private String barcode;

    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "selling_price", precision = 12, scale = 2)
    private BigDecimal sellingPrice;

    @Column(name = "description", length = 500)
    private String description;

    @Builder.Default
    @Column(name = "current_stock", precision = 10, scale = 3)
    private BigDecimal currentStock = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "min_stock", precision = 10, scale = 3)
    private BigDecimal minStock = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
