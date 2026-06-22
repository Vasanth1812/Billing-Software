package com.Billing_System.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * A single row in the Payables Aging Report table.
 * Aging buckets: current (0-30d), 31-60d, 61-90d, 90+ days.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayablesAgingRowDTO {

    private String id;
    private String vendorCode;
    private String vendorName;
    private BigDecimal totalOutstanding;
    private BigDecimal current;       // 0-30 days
    private BigDecimal days31to60;
    private BigDecimal days61to90;
    private BigDecimal over90Days;
    private int invoiceCount;         // number of unpaid invoices
    private String oldestInvoiceDate;
    private String status;            // "NORMAL", "OVERDUE", "CRITICAL"
    private String lastPaymentDate;
}
