package com.Billing_System.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "bulk_upload_rows",
        indexes = {
                @Index(name = "idx_bulk_upload_rows_upload", columnList = "upload_id"),
                @Index(name = "idx_bulk_upload_rows_status", columnList = "status")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkUploadRow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "upload_id", nullable = false)
    @ToString.Exclude
    private BulkUpload upload;

    @Column(name = "row_number", nullable = false)
    private int rowNumber;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    @ToString.Exclude
    private Product product;

    @Column(name = "product_name", length = 200)
    private String productName;

    @Column(name = "sku_barcode", length = 50)
    private String skuBarcode;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "unit_of_measure", length = 20)
    private String unitOfMeasure;

    @Column(name = "purchase_rate", precision = 10, scale = 2)
    private BigDecimal purchaseRate;

    @Column(name = "mrp", precision = 10, scale = 2)
    private BigDecimal mrp;

    @Column(name = "gst_rate", precision = 5, scale = 2)
    private BigDecimal gstRate;

    @Column(name = "hsn_code", length = 20)
    private String hsnCode;

    @Column(name = "opening_stock", precision = 10, scale = 3)
    private BigDecimal openingStock;

    @Column(name = "min_stock", precision = 10, scale = 3)
    private BigDecimal minStock;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "supplier_name", length = 200)
    private String supplierName;

    @Column(name = "expiry", length = 100)
    private String expiry;

    @Column(name = "active", length = 20)
    private String active;

    public UUID getProductId() {
        return product != null ? product.getId() : null;
    }
}
