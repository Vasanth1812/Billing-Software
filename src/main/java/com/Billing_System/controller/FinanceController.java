package com.Billing_System.controller;

import com.Billing_System.dto.VendorInvoiceRequestDTO;
import com.Billing_System.dto.VendorInvoiceResponseDTO;
import com.Billing_System.dto.VendorPaymentRequestDTO;
import com.Billing_System.dto.VendorPaymentResponseDTO;
import com.Billing_System.service.FinanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FinanceController {

    private final FinanceService financeService;

    // ─── Invoices ──────────────────────────────────────────────────────────────

    @PostMapping("/invoices")
    public ResponseEntity<VendorInvoiceResponseDTO> submitInvoice(@Valid @RequestBody VendorInvoiceRequestDTO request) {
        VendorInvoiceResponseDTO response = financeService.submitInvoice(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/invoices")
    public ResponseEntity<List<VendorInvoiceResponseDTO>> getAllInvoices() {
        return ResponseEntity.ok(financeService.getAllInvoices());
    }

    /** Bug 2 Fix: GET single invoice by ID */
    @GetMapping("/invoices/{id}")
    public ResponseEntity<VendorInvoiceResponseDTO> getInvoiceById(@PathVariable UUID id) {
        return ResponseEntity.ok(financeService.getInvoiceById(id));
    }

    @PutMapping("/invoices/{id}/approve")
    public ResponseEntity<VendorInvoiceResponseDTO> approveInvoice(@PathVariable UUID id) {
        VendorInvoiceResponseDTO response = financeService.approveInvoice(id);
        return ResponseEntity.ok(response);
    }

    // ─── Payments ──────────────────────────────────────────────────────────────

    @PostMapping("/payments")
    public ResponseEntity<VendorPaymentResponseDTO> createPayment(
            @Valid @RequestBody VendorPaymentRequestDTO request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        VendorPaymentResponseDTO response = financeService.createPayment(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/payments")
    public ResponseEntity<List<VendorPaymentResponseDTO>> getAllPayments() {
        return ResponseEntity.ok(financeService.getAllVendorPayments());
    }

    /** Bug 2 Fix: GET single payment by ID — corrects fetchPaymentById() in vendorService.js */
    @GetMapping("/payments/{id}") 
    public ResponseEntity<VendorPaymentResponseDTO> getPaymentById(@PathVariable UUID id) {
        return ResponseEntity.ok(financeService.getPaymentById(id));
    }

    @GetMapping("/payments/vendor/{vendorId}")
    public ResponseEntity<List<VendorPaymentResponseDTO>> getPaymentsByVendor(@PathVariable UUID vendorId) {
        return ResponseEntity.ok(financeService.getPaymentsByVendor(vendorId));
    }
}
