package com.Billing_System.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class VendorPaymentResponseDTO {
    private UUID id;
    private String paymentNumber;
    private UUID invoiceId;
    private String invoiceNumber;
    private UUID vendorId;
    private String vendorName;
    
    private String paymentMode;
    private BigDecimal paymentAmount;
    private BigDecimal holdAmount;
    private BigDecimal itcHoldAmount;
    private BigDecimal netPayment;
    private String holdReason;
    
    private LocalDate paymentDueDate;
    private UUID bankAccountId;
    private String status;
    private String bankReference;
    
    private LocalDateTime createdAt;
}
