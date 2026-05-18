package com.Billing_System.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class VendorInvoiceRequestDTO {
    @NotBlank(message = "Invoice number is required")
    private String invoiceNumber;

    @NotNull(message = "Vendor ID is required")
    private UUID vendorId;

    @NotNull(message = "GRN ID is required")
    private UUID grnId;

    @NotNull(message = "Purchase Order ID is required")
    private UUID purchaseOrderId;

    @NotNull(message = "Invoice date is required")
    private LocalDate invoiceDate;

    @NotNull(message = "Due date is required")
    private LocalDate dueDate;

    @NotNull(message = "Invoice amount is required")
    private BigDecimal invoiceAmount;

    @NotNull(message = "GST amount is required")
    private BigDecimal gstAmount;

    private String irnNumber;
}
