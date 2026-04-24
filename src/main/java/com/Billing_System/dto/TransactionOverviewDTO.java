package com.Billing_System.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionOverviewDTO {
    private UUID id;
    private String type; // "PURCHASE" or "SALE"
    private String partyName; // Supplier Name or Customer Name
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private BigDecimal amount;
    private String status;
    private LocalDateTime createdAt;
}
