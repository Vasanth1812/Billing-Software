package com.Billing_System.service;

import com.Billing_System.dto.PaymentRequestDTO;
import com.Billing_System.dto.PaymentResponseDTO;
import com.Billing_System.entity.Payment;
import com.Billing_System.entity.SalesInvoice;
import com.Billing_System.repository.PaymentRepository;
import com.Billing_System.repository.SalesInvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentService {

    private static final Set<String> VALID_METHODS = Set.of(
            "CASH", "UPI", "CARD", "NEFT", "RTGS", "CHEQUE", "RAZORPAY", "PHONEPE", "OTHER"
    );

    private final PaymentRepository    paymentRepository;
    private final SalesInvoiceRepository invoiceRepository;

    // ─── Record a Payment ──────────────────────────────────────────────────────

    /**
     * Records a manual payment (Cash / UPI / NEFT / Cheque / Card).
     *
     * Business rules enforced:
     *  1. Payment method must be one of the allowed values.
     *  2. If linked to an invoice, amount cannot exceed the remaining balance.
     *  3. After saving, invoice.amountPaid is recalculated from DB SUM (safe, no drift).
     *  4. If amountPaid >= grandTotal → invoice status → PAID automatically.
     */
    public PaymentResponseDTO recordPayment(PaymentRequestDTO dto) {
        validateMethod(dto.getMethod());

        SalesInvoice invoice = null;

        if (dto.getInvoiceId() != null) {
            invoice = invoiceRepository.findById(dto.getInvoiceId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Invoice not found: " + dto.getInvoiceId()));

            // nullSafe: existing DB rows may have NULL for grandTotal / amountPaid
            BigDecimal grandTotal  = nullSafe(invoice.getGrandTotal());
            BigDecimal alreadyPaid = nullSafe(invoice.getAmountPaid());
            BigDecimal balance     = grandTotal.subtract(alreadyPaid);

            if (dto.getAmount().compareTo(balance) > 0) {
                throw new IllegalArgumentException(
                        "Payment amount (" + dto.getAmount() + ") exceeds outstanding balance ("
                        + balance + "). Maximum payable: \u20b9" + balance);
            }
        }

        // Build and save the Payment record
        Payment payment = Payment.builder()
                .invoice(invoice)
                .amount(dto.getAmount())
                .paymentDate(dto.getPaymentDate() != null ? dto.getPaymentDate() : LocalDate.now())
                .method(dto.getMethod().toUpperCase())
                .referenceNumber(dto.getReferenceNumber())
                .customerName(dto.getCustomerName())
                .customerPhone(dto.getCustomerPhone())
                .notes(dto.getNotes())
                .build();

        Payment saved = paymentRepository.save(payment);
        log.info("Payment recorded: ₹{} via {} for invoice {}",
                saved.getAmount(), saved.getMethod(),
                invoice != null ? invoice.getInvoiceNumber() : "walk-in");

        // ── Update invoice balance (if linked) ────────────────────────────────
        if (invoice != null) {
            updateInvoiceBalance(invoice);
        }

        return buildResponse(saved, invoice);
    }

    // ─── Read Operations ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Payment> getAllPayments() {
        return paymentRepository.findAllByOrderByPaymentDateDesc();
    }

    @Transactional(readOnly = true)
    public Payment getPaymentById(UUID id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByInvoice(UUID invoiceId) {
        if (!invoiceRepository.existsById(invoiceId)) {
            throw new IllegalArgumentException("Invoice not found: " + invoiceId);
        }
        return paymentRepository.findByInvoiceId(invoiceId);
    }

    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByDateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("'from' date must be before or equal to 'to' date");
        }
        return paymentRepository.findByPaymentDateBetweenOrderByPaymentDateDesc(from, to);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTodayTotal() {
        return paymentRepository.sumByDate(LocalDate.now());
    }

    // ─── Private Helpers ───────────────────────────────────────────────────────

    /**
     * Recomputes invoice.amountPaid from the DB SUM of all payments.
     * This prevents drift if a payment is ever voided/edited.
     */
    private void updateInvoiceBalance(SalesInvoice invoice) {
        BigDecimal totalPaid  = nullSafe(paymentRepository.sumAmountByInvoiceId(invoice.getId()));
        BigDecimal grandTotal = nullSafe(invoice.getGrandTotal());
        invoice.setAmountPaid(totalPaid);

        BigDecimal balance = grandTotal.subtract(totalPaid);

        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            invoice.setStatus("PAID");
            log.info("Invoice {} fully paid. Status → PAID", invoice.getInvoiceNumber());
        } else {
            invoice.setStatus("CREDIT");
            log.info("Invoice {} partially paid. Balance: \u20b9{}", invoice.getInvoiceNumber(), balance);
        }

        invoiceRepository.save(invoice);
    }

    private void validateMethod(String method) {
        if (method == null || !VALID_METHODS.contains(method.toUpperCase())) {
            throw new IllegalArgumentException(
                    "Invalid payment method: '" + method
                    + "'. Allowed: CASH, UPI, CARD, NEFT, RTGS, CHEQUE, RAZORPAY, PHONEPE, OTHER");
        }
    }

    /** Null-safe BigDecimal — returns ZERO if value is null (handles legacy DB rows) */
    private BigDecimal nullSafe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private PaymentResponseDTO buildResponse(Payment payment, SalesInvoice invoice) {
        PaymentResponseDTO.PaymentResponseDTOBuilder builder = PaymentResponseDTO.builder()
                .paymentId(payment.getId())
                .amount(payment.getAmount())
                .paymentDate(payment.getPaymentDate())
                .method(payment.getMethod())
                .referenceNumber(payment.getReferenceNumber())
                .gatewayTxnId(payment.getGatewayTxnId())
                .gatewayStatus(payment.getGatewayStatus())
                .customerName(payment.getCustomerName())
                .customerPhone(payment.getCustomerPhone())
                .notes(payment.getNotes())
                .createdAt(payment.getCreatedAt());

        if (invoice != null) {
            BigDecimal grandTotal = nullSafe(invoice.getGrandTotal());
            BigDecimal amountPaid = nullSafe(invoice.getAmountPaid());
            BigDecimal balance = grandTotal.subtract(amountPaid);
            builder.invoiceId(invoice.getId())
                   .invoiceNumber(invoice.getInvoiceNumber())
                   .grandTotal(grandTotal)
                   .amountPaid(amountPaid)
                   .balanceDue(balance.max(BigDecimal.ZERO))
                   .invoiceStatus(invoice.getStatus());
        }

        return builder.build();
    }
}
