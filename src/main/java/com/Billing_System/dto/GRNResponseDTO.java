package com.Billing_System.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class GRNResponseDTO {
    private UUID id;
    private String grnNumber;
    private UUID purchaseOrderId;
    private String purchaseOrderNumber;
    
    private UUID vendorId;
    private String vendorName;
    
    private LocalDateTime receivedDate;
    private UUID receivedByUserId;
    private String receivedByUserName;
    private String status;
    private String vendorInvoiceNumber;
    private String remarks;
    private LocalDateTime createdAt;
    
    // Shortage Report Details
    private String shortageReportNumber;
    private java.math.BigDecimal totalShortageValue;
    
    private List<GRNItemResponseDTO> items;
}
