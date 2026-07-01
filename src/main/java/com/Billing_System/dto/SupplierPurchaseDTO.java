package com.Billing_System.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierPurchaseDTO {
    private UUID id;
    private String supplier;
    private LocalDate date;
    private String invoiceNo;
    private int items;
    private BigDecimal gross;
    private BigDecimal tax;
    private BigDecimal net;
}
