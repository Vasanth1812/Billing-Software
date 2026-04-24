package com.Billing_System.controller;

import java.util.Optional;

import com.Billing_System.dto.PurchaseRequestDTO;
import com.Billing_System.dto.TransactionOverviewDTO;
import com.Billing_System.entity.PurchaseOrder;
import com.Billing_System.entity.SalesInvoice;
import com.Billing_System.repository.SalesInvoiceRepository;
import com.Billing_System.service.PurchaseService;
import com.Billing_System.service.SalesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/purchases")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PurchaseController {

    private final PurchaseService purchaseService;
    private final SalesService salesService;
    private final SalesInvoiceRepository salesInvoiceRepository;
    private final com.Billing_System.repository.PurchaseOrderRepository purchaseOrderRepository;

    /**
     * GET /api/purchases
     * List all purchase orders, newest first
     */
    @GetMapping
    public ResponseEntity<List<TransactionOverviewDTO>> getAllPurchases() {
        return ResponseEntity.ok(purchaseService.getAllPurchases());
    }

    /**
     * GET /api/purchases/{id}
     * View single purchase order with all line items
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getPurchaseById(@PathVariable UUID id) {
        // Try searching in Purchase Orders first
        Optional<PurchaseOrder> purchase = purchaseOrderRepository.findByIdWithDetails(id);
        if (purchase.isPresent()) {
            return ResponseEntity.ok(purchase.get());
        }

        // If not found, try searching in Sales Invoices (since we merged them in the
        // list)
        Optional<SalesInvoice> sale = salesInvoiceRepository.findByIdWithItems(id);
        if (sale.isPresent()) {
            return ResponseEntity.ok(sale.get());
        }

        return ResponseEntity.notFound().build();
    }

    /**
     * POST /api/purchases
     * Save purchase order + auto-update stock + ledger entries
     */
    @PostMapping
    public ResponseEntity<PurchaseOrder> savePurchase(@Valid @RequestBody PurchaseRequestDTO dto) {
        PurchaseOrder saved = purchaseService.savePurchase(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
