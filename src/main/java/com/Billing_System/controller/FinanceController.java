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

import java.util.UUID;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
public class FinanceController {

    private final FinanceService financeService;

    @PostMapping("/invoices")
    public ResponseEntity<VendorInvoiceResponseDTO> submitInvoice(@Valid @RequestBody VendorInvoiceRequestDTO request) {
        VendorInvoiceResponseDTO response = financeService.submitInvoice(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/payments")
    public ResponseEntity<VendorPaymentResponseDTO> createPayment(
            @Valid @RequestBody VendorPaymentRequestDTO request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        
        VendorPaymentResponseDTO response = financeService.createPayment(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
