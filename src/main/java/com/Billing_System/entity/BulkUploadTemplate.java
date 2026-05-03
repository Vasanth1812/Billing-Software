package com.Billing_System.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bulk_upload_templates",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_bulk_upload_template_supplier", columnNames = "normalized_supplier_name")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkUploadTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    @ToString.Exclude
    private Supplier supplier;

    @Column(name = "supplier_name_snapshot", nullable = false, length = 200)
    private String supplierNameSnapshot;

    @Column(name = "normalized_supplier_name", nullable = false, length = 200)
    private String normalizedSupplierName;

    @Builder.Default
    @Column(name = "template_name", nullable = false, length = 100)
    private String templateName = "Products Master";

    @Column(name = "headers_json", nullable = false, columnDefinition = "TEXT")
    private String headersJson;

    @Column(name = "column_count", nullable = false)
    private int columnCount;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "last_used_at", nullable = false)
    private LocalDateTime lastUsedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_upload_id")
    @ToString.Exclude
    private BulkUpload sourceUpload;
}
