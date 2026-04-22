package com.Billing_System.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class SalesInvoiceResponseDTO {

    private UUID id;
    private String invoiceNumber;
    private String customerName;
    private String customerPhone;
    private String customerGstin;
    private LocalDate invoiceDate;
    private BigDecimal subtotal;
    private BigDecimal cgstAmount;
    private BigDecimal sgstAmount;
    private BigDecimal grandTotal;
    private String paymentMode;
    private String status;
    private LocalDateTime createdAt;
    private List<SaleItemResponseDTO> items;

    @Data
    @Builder
    public static class SaleItemResponseDTO {
        private UUID id;
        private String productName;
        private String hsnCode;
        private String sku;
        private BigDecimal quantity;
        private BigDecimal mrp;
        private BigDecimal discountPct;
        private BigDecimal gstRate;
        private BigDecimal netAmount;
    }
}
