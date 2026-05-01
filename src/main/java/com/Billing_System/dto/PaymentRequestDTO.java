package com.Billing_System.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request body for recording a payment.
 * Used for both manual payments (Cash/UPI/NEFT) and updating gateway payments.
 */
@Data
public class PaymentRequestDTO {

    /** Invoice to settle. Nullable for walk-in cash payments without invoice. */
    private UUID invoiceId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotNull(message = "Payment date is required")
    private LocalDate paymentDate;

    /**
     * Payment method.
     * Allowed: CASH | UPI | CARD | NEFT | RTGS | CHEQUE | RAZORPAY | PHONEPE | OTHER
     */
    @NotBlank(message = "Payment method is required")
    private String method;

    /** UPI UTR / cheque number / bank ref / Razorpay payment_id etc. */
    private String referenceNumber;

    /** Optional: walk-in customer name */
    private String customerName;

    /** Optional: walk-in customer phone */
    private String customerPhone;

    /** Internal notes */
    private String notes;
}
