package com.Billing_System.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Response returned after a bulk product import.
 * Tells the frontend exactly how many rows succeeded, skipped, and failed.
 */
@Data
@Builder
public class BulkImportResponseDTO {

    private int totalRows;              // total data rows in the uploaded XLSX
    private int successCount;           // rows successfully inserted
    private int skippedCount;           // rows skipped (exact SKU already exists in DB)
    private int failedCount;            // rows that threw a validation error
    private int duplicateBarcodeCount;  // products imported with duplicate barcodes (need cleanup)

    private List<String> skippedSkus;   // SKUs that were skipped (already in DB)
    private List<String> errors;        // error messages for failed rows

    /**
     * Hint message shown to user when duplicate barcodes are detected.
     * Frontend "Duplicates" button uses GET /api/products/duplicates to fetch them.
     */
    public String getDuplicateHint() {
        if (duplicateBarcodeCount > 0) {
            return duplicateBarcodeCount + " product(s) have duplicate barcodes. "
                    + "Click 'Duplicates' button to view and edit them.";
        }
        return null;
    }
}
