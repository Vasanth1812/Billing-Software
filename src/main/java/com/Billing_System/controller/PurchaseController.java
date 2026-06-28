package com.Billing_System.controller;

import java.time.LocalDate;
import java.util.Map;
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
import org.springframework.jdbc.core.JdbcTemplate;

@RestController
@RequestMapping("/api/purchase-orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PurchaseController {

    private final JdbcTemplate jdbcTemplate;

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

    /**
     * PUT /api/purchase-orders/{id}
     * Update existing purchase order
     */
    @PutMapping("/{id}")
    public ResponseEntity<PurchaseOrder> updatePurchase(@PathVariable UUID id, @Valid @RequestBody PurchaseRequestDTO dto) {
        PurchaseOrder updated = purchaseService.updatePurchase(id, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * PUT /api/purchase-orders/{id}/status
     * Update purchase order status
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<PurchaseOrder> updateStatus(@PathVariable UUID id, @RequestParam String status) {
        PurchaseOrder updated = purchaseService.updateStatus(id, status);
        return ResponseEntity.ok(updated);
    }

    /**
     * PUT /api/purchase-orders/{id}/vendor-response
     * Vendor responds to a PO (Accept/Decline) with an optional expected delivery date
     */
    @PostMapping("/{id}/respond")
    public ResponseEntity<?> respondToPO(
            @PathVariable UUID id,
            @RequestBody Map<String, String> payload) {
        String status = payload.get("status");
        String deliveryDateStr = payload.get("expectedDeliveryDate");
        String vendorNotes = payload.get("vendorNotes");
        LocalDate deliveryDate = deliveryDateStr != null ? LocalDate.parse(deliveryDateStr) : null;
        PurchaseOrder updated = purchaseService.vendorRespondToPO(id, status, deliveryDate, vendorNotes);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/purchase-orders/{id}
     * Delete a purchase order
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePurchase(@PathVariable UUID id) {
        purchaseService.deletePurchase(id);
        return ResponseEntity.noContent().build();
    }
}
