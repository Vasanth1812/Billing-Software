package com.Billing_System.controller;

import com.Billing_System.dto.PurchaseRequestDTO;
import com.Billing_System.entity.PurchaseOrder;
import com.Billing_System.service.PurchaseService;
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

public class PurchaseController {

    private final PurchaseService purchaseService;

    /**
     * GET /api/purchases
     * List all purchase orders, newest first
     */
    @GetMapping
    public ResponseEntity<List<PurchaseOrder>> getAllPurchases() {
        return ResponseEntity.ok(purchaseService.getAllPurchases());
    }

    /**
     * GET /api/purchases/{id}
     * View single purchase order with all line items
     */
    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrder> getPurchaseById(@PathVariable UUID id) {
        return ResponseEntity.ok(purchaseService.getPurchaseById(id));
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
