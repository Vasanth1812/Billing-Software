package com.Billing_System.controller;

import com.Billing_System.dto.InventorySummaryDTO;
import com.Billing_System.dto.StockAdjustmentDTO;
import com.Billing_System.entity.StockLedger;
import com.Billing_System.service.InventoryService;
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
}
