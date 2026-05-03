package com.Billing_System.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class BulkUploadDetailDTO {
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
    private List<RowDTO> rows;

    @Data
    @Builder
    public static class RowDTO {
        private UUID id;
        private int rowNumber;
        private String status;
        private String errorMessage;
        private UUID productId;
        private String productName;
        private String skuBarcode;
        private String category;
        private String unitOfMeasure;
        private BigDecimal purchaseRate;
        private BigDecimal mrp;
        private BigDecimal gstRate;
        private String hsnCode;
        private BigDecimal openingStock;
        private BigDecimal minStock;
        private String description;
        private String brand;
        private String supplierName;
        private String expiry;
        private String active;
    }
}
