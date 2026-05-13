package com.Billing_System.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class StoResponseDTO {
    private UUID id;
    private String stoNumber;
    
    private UUID productId;
    private String productName;
    
    private String sourceBranchName;
    private String destBranchName;
    
    private BigDecimal transferQuantity;
    private LocalDate transferDate;
    
    private String status;
    private String transferMode;
    private String priority;
    private BigDecimal capitalSaved;
    
    private UUID createdById;
    private String createdByName;
    
    private UUID approvedById;
    private String approvedByName;
    
    private LocalDateTime createdAt;
}
