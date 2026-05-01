package com.Billing_System.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response returned after recording a payment.
 * Includes updated invoice balance so the frontend can refresh the UI immediately.
 */
@Data
@Builder
public class PaymentResponseDTO {

    // ── Payment Details ─────────────────────────────────────────────────────────
    private UUID    paymentId;
    private BigDecimal amount;
    private LocalDate  paymentDate;
    private String     method;
    private String     referenceNumber;
    private String     gatewayTxnId;
    private String     gatewayStatus;
    private String     customerName;
    private String     customerPhone;
    private String     notes;
    private LocalDateTime createdAt;

    // ── Invoice Summary (updated after payment) ─────────────────────────────────
    private UUID       invoiceId;
    private String     invoiceNumber;
    private BigDecimal grandTotal;       // invoice total
    private BigDecimal amountPaid;       // total paid so far (all payments combined)
    private BigDecimal balanceDue;       // grandTotal - amountPaid
    private String     invoiceStatus;    // PAID | CREDIT
}
