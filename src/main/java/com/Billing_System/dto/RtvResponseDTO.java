package com.Billing_System.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class RtvResponseDTO {
    private UUID id;
    private String rtvNumber;
    private UUID grnId;
    private String grnNumber;
    private UUID purchaseOrderId;
    private String purchaseOrderNumber;
    private UUID vendorId;
    private String vendorName;
    
    private String status;
    private BigDecimal totalReturnValue;
    
    private UUID shortageReportId;
    private String shortageReportNumber;
    
    private String disputeNote;
    private LocalDateTime resolvedAt;
    
    private UUID createdById;
    private String createdByName;
    private LocalDateTime createdAt;
    
    private java.util.List<RtvProductDTO> returnedProducts;
}
