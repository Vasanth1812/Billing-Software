package com.Billing_System.controller;

import com.Billing_System.dto.*;
import com.Billing_System.entity.StockLedger;
import com.Billing_System.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/summary")
    public ResponseEntity<InventorySummaryDTO> getInventorySummary() {
        return ResponseEntity.ok(inventoryService.getInventorySummary());
    }

    @GetMapping("/products")
    public ResponseEntity<List<com.Billing_System.dto.InventoryProductDTO>> getInventoryProducts(
            @RequestParam(required = false, defaultValue = "ALL") String status) {
        return ResponseEntity.ok(inventoryService.getInventoryProducts(status));
    }

    @PostMapping("/adjust")
    public ResponseEntity<java.util.Map<String, String>> adjustStock(
            @jakarta.validation.Valid @RequestBody StockAdjustmentDTO dto) {
        inventoryService.adjustStock(dto);
        return ResponseEntity.ok(java.util.Map.of("message", "Stock adjusted successfully"));
    }

    @GetMapping("/history/{productId}")
    public ResponseEntity<List<StockLedger>> getStockHistory(@PathVariable UUID productId) {
        return ResponseEntity.ok(inventoryService.getStockHistory(productId));
    }

    // --- Bin Location APIs ---

    @PostMapping("/bins")
    public ResponseEntity<BinLocationResponseDTO> createBinLocation(@Valid @RequestBody BinLocationRequestDTO request) {
        return ResponseEntity.status(201).body(inventoryService.createBinLocation(request));
    }

    @GetMapping("/bins")
    public ResponseEntity<List<BinLocationResponseDTO>> getAllBinLocations() {
        return ResponseEntity.ok(inventoryService.getAllBinLocations());
    }

    // --- STO (Stock Transfer Order) APIs ---

    @PostMapping("/sto")
    public ResponseEntity<StoResponseDTO> createSTO(
            @Valid @RequestBody StoRequestDTO request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        return ResponseEntity.status(201).body(inventoryService.createSTO(request, userId));
    }

    @GetMapping("/sto")
    public ResponseEntity<List<StoResponseDTO>> getAllSTOs() {
        return ResponseEntity.ok(inventoryService.getAllSTOs());
    }

    @PatchMapping("/sto/{id}/status")
    public ResponseEntity<StoResponseDTO> updateSTOStatus(
            @PathVariable UUID id,
            @RequestParam String status,
            @RequestHeader(value = "X-User-Id", required = false) UUID approverId) {
        return ResponseEntity.ok(inventoryService.updateSTOStatus(id, status, approverId));
    }
}
