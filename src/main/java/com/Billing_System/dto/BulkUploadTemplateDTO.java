package com.Billing_System.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class BulkUploadTemplateDTO {
    private UUID id;
    private UUID supplierId;
    private String supplierName;
    private String templateName;
    private int columnCount;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
    private UUID sourceUploadId;
    private List<String> headers;
}
