package com.Billing_System.controller;

import com.Billing_System.dto.PaymentRequestDTO;
import com.Billing_System.dto.PaymentResponseDTO;
import com.Billing_System.entity.Payment;
import com.Billing_System.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Payment API — records and tracks all money received.
 *
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │ POST   /api/payments                → Record a new payment              │
 * │ GET    /api/payments                → List all payments                 │
 * │ GET    /api/payments/{id}           → Single payment detail             │
 * │ GET    /api/payments/invoice/{id}   → All payments for one invoice      │
 * │ GET    /api/payments/by-date        → Payments in a date range          │
 * │ GET    /api/payments/today-total    → Today's total collections (KPI)   │
 * └─────────────────────────────────────────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;

    // ──────────────────────────────────────────────────────────────────────────
    // WRITE
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * POST /api/payments
     *
     * Records a payment. Can be:
     *   - Immediate (Cash/UPI at POS counter)
     *   - Credit settlement (customer pays after purchase)
     *   - Partial (part-payment, balance remains as CREDIT)
     *
     * Request body:
     * {
     *   "invoiceId"       : "uuid",          // optional — omit for walk-in cash
     *   "amount"          : 500.00,          // required
     *   "paymentDate"     : "2025-05-01",    // required
     *   "method"          : "UPI",           // CASH|UPI|CARD|NEFT|RTGS|CHEQUE|OTHER
     *   "referenceNumber" : "UPI123456789",  // optional
     *   "customerName"    : "Ravi Kumar",    // optional
     *   "customerPhone"   : "9876543210",    // optional
     *   "notes"           : "Part payment"   // optional
     * }
     *
     * Response includes updated invoice balance so frontend shows current state.
     */
    @PostMapping
    public ResponseEntity<PaymentResponseDTO> recordPayment(
            @Valid @RequestBody PaymentRequestDTO dto) {
        PaymentResponseDTO response = paymentService.recordPayment(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // READ
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * GET /api/payments
     * List all payments, newest first.
     */
    @GetMapping
    public ResponseEntity<List<Payment>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    /**
     * GET /api/payments/{id}
     * Single payment detail (for receipt screen).
     */
    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPaymentById(@PathVariable UUID id) {
        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }

    /**
     * GET /api/payments/invoice/{invoiceId}
     *
     * All payments for a specific invoice — shows payment history.
     * Used by the "Invoice Detail" screen to render payment timeline.
     *
     * Example response when customer paid in 2 instalments:
     * [
     *   { "amount": 500, "method": "CASH",  "paymentDate": "2025-04-30" },
     *   { "amount": 300, "method": "UPI",   "paymentDate": "2025-05-01" }
     * ]
     */
    @GetMapping("/invoice/{invoiceId}")
    public ResponseEntity<List<Payment>> getPaymentsByInvoice(@PathVariable UUID invoiceId) {
        return ResponseEntity.ok(paymentService.getPaymentsByInvoice(invoiceId));
    }

    /**
     * GET /api/payments/by-date?from=YYYY-MM-DD&to=YYYY-MM-DD
     * Payments within a date range. Used for daily/weekly reports.
     */
    @GetMapping("/by-date")
    public ResponseEntity<List<Payment>> getPaymentsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(paymentService.getPaymentsByDateRange(from, to));
    }

    /**
     * GET /api/payments/today-total
     *
     * Total money collected today. Used by the dashboard KPI card.
     *
     * Response: { "date": "2025-05-01", "total": 12500.00 }
     */
    @GetMapping("/today-total")
    public ResponseEntity<Map<String, Object>> getTodayTotal() {
        BigDecimal total = paymentService.getTodayTotal();
        return ResponseEntity.ok(Map.of(
                "date",  LocalDate.now().toString(),
                "total", total
        ));
    }
}
