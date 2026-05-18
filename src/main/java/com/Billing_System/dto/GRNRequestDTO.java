package com.Billing_System.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class GRNRequestDTO {

    @NotNull(message = "Purchase Order ID is required")
    private UUID purchaseOrderId;

    @NotNull(message = "Received Date is required")
    private LocalDateTime receivedDate;

    private String vendorInvoiceNumber;
    private String remarks;
    private String status;

    private List<GRNItemRequestDTO> items;
}
