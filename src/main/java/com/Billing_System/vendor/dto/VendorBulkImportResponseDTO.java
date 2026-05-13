package com.Billing_System.vendor.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for vendor product bulk import.
 * Same structure as BulkImportResponseDTO but for vendor products.
 */
@Data
@Builder
public class VendorBulkImportResponseDTO {

    private UUID   uploadId;
    private String fileName;
    private int    totalRows;
    private int    successCount;
    private int    failedCount;
    private int    skippedCount;       // duplicate vendor SKUs skipped
    private int    updatedCount;       // existing vendor SKUs updated
    private String status;             // SUCCESS | PARTIAL | FAILED
    private LocalDateTime processedAt;

    private List<String> errors;       // row-level error messages
    private List<String> skippedSkus;  // vendor SKUs that were skipped (duplicates)
    private List<String> vendorCodesNotFound; // vendor codes in Excel not found in DB
}
