package com.Billing_System.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class VendorInvoiceResponseDTO {
    private UUID id;
    private String invoiceNumber;
    private UUID vendorId;
    private String vendorName;
    private UUID grnId;
    private String grnNumber;
    private UUID purchaseOrderId;
    private String purchaseOrderNumber;
    
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private BigDecimal invoiceAmount;
    private BigDecimal gstAmount;
    private BigDecimal totalAmount;
    
    private String poMatchStatus;
    private String grnMatchStatus;
    private String invoiceMatchStatus;
    private boolean threeWayMatch;
    
    private String irnNumber;
    private String submissionStatus;
    private LocalDateTime createdAt;
}
