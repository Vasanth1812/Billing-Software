package com.Billing_System.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bulk_uploads")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Builder.Default
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    @Column(name = "total_rows", nullable = false)
    private int totalRows;

    @Column(name = "success_count", nullable = false)
    private int successCount;

    @Column(name = "failed_count", nullable = false)
    private int failedCount;

    @Column(name = "skipped_count", nullable = false)
    private int skippedCount;

    @Column(name = "duplicate_barcode_count", nullable = false)
    private int duplicateBarcodeCount;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "auto_create_suppliers", nullable = false)
    private boolean autoCreateSuppliers;
}
