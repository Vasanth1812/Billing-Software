package com.Billing_System.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents ONE payment transaction against a SalesInvoice.
 *
 * Why a separate table?
 *   - A customer can pay an invoice in MULTIPLE instalments (partial payments).
 *   - Each payment can use a different method (e.g. part Cash + part UPI).
 *   - Razorpay / PhonePe webhooks need to record gateway_txn_id and status.
 *   - SalesInvoice.amountPaid = SUM of all Payment.amount for that invoice.
 *
 * Flow:
 *   Credit sale created → SalesInvoice.status = "CREDIT", amountPaid = 0
 *   Customer pays ₹500 cash → Payment saved, amountPaid updated to 500
 *   Customer pays remaining → Payment saved, amountPaid = grandTotal → status = "PAID"
 */
@Entity
@Table(name = "payments",
        indexes = {
            @Index(name = "idx_payments_invoice", columnList = "invoice_id"),
            @Index(name = "idx_payments_date",    columnList = "payment_date")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * The invoice this payment settles (fully or partially).
     * Nullable: walk-in cash payments sometimes don't have an invoice yet.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    @ToString.Exclude
    private SalesInvoice invoice;

    /** Amount paid in this single transaction */
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /** Date of payment (may differ from invoice date for credit sales) */
    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    /**
     * Payment method.
     * Allowed values: CASH | UPI | CARD | NEFT | RTGS | CHEQUE | RAZORPAY | PHONEPE | OTHER
     */
    @Column(name = "method", nullable = false, length = 20)
    private String method;

    /**
     * Transaction reference number.
     * UPI: UTR / transaction ID (e.g. "UPI123456789012")
     * NEFT/RTGS: bank reference number
     * Cheque: cheque number
     * Razorpay: payment_id from webhook
     * PhonePe: transactionId from webhook
     * Cash: optional receipt number
     */
    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    /**
     * Gateway-specific transaction ID.
     * Razorpay: razorpay_payment_id
     * PhonePe: transactionId
     * Null for offline payments (Cash / UPI / NEFT).
     */
    @Column(name = "gateway_txn_id", length = 100)
    private String gatewayTxnId;

    /**
     * Gateway payment status.
     * Values: SUCCESS | PENDING | FAILED | REFUNDED
     * Null for offline payments.
     */
    @Column(name = "gateway_status", length = 20)
    private String gatewayStatus;

    /** Optional: customer name (for walk-in sales without a formal invoice) */
    @Column(name = "customer_name", length = 100)
    private String customerName;

    /** Optional: customer phone (for WhatsApp receipt sending later) */
    @Column(name = "customer_phone", length = 15)
    private String customerPhone;

    /** Internal notes (e.g. "Customer paid in two parts" / "Cheque bounced") */
    @Column(name = "notes", length = 500)
    private String notes;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
