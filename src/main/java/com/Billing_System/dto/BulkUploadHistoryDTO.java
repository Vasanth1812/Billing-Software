package com.Billing_System.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class BulkUploadHistoryDTO {
    private UUID id;
    private String fileName;
    private LocalDateTime uploadedAt;
    private int totalRows;
    private int successCount;
    private int failedCount;
    private int skippedCount;
    private int duplicateBarcodeCount;
    private String status;
    private boolean autoCreateSuppliers;
}
