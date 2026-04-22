package com.Billing_System.controller;

import com.Billing_System.dto.SaleRequestDTO;
import com.Billing_System.dto.SalesInvoiceResponseDTO;
import com.Billing_System.entity.SalesInvoice;
import com.Billing_System.service.SalesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SalesController {

    private final SalesService salesService;

    /**
     * GET /api/sales
     * List all sales invoices, newest first
     */
    @GetMapping
    public ResponseEntity<List<SalesInvoice>> getAllSales() {
        return ResponseEntity.ok(salesService.getAllSales());
    }

    /**
     * GET /api/sales/{id}
     * View single invoice with all line items
     */
    @GetMapping("/{id}")
    public ResponseEntity<SalesInvoice> getSaleById(@PathVariable UUID id) {
        return ResponseEntity.ok(salesService.getSaleById(id));
    }

    /**
     * GET /api/sales/{id}/print
     * Get printable invoice data suitable for PDF rendering
     */
    @GetMapping("/{id}/print")
    public ResponseEntity<SalesInvoice> getPrintableInvoice(@PathVariable UUID id) {
        return ResponseEntity.ok(salesService.getPrintableInvoice(id));
    }

    /**
     * GET /api/sales/by-date?from=YYYY-MM-DD&to=YYYY-MM-DD
     * Get invoices in a date range (used for reporting)
     */
    @GetMapping("/by-date")
    public ResponseEntity<List<SalesInvoiceResponseDTO>> getSalesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(salesService.getSalesByDateRange(from, to));
    }

    /**
     * POST /api/sales
     * Save sale + reduce stock + write ledger entries (atomic)
     */
    @PostMapping
    public ResponseEntity<SalesInvoice> saveSale(@Valid @RequestBody SaleRequestDTO dto) {
        SalesInvoice saved = salesService.saveSale(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
