package com.Billing_System.controller;

import com.Billing_System.dto.*;
import com.Billing_System.vendor.entity.VendorCategory;
import com.Billing_System.service.WarehouseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/warehouse")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping("/categories")
    public ResponseEntity<List<VendorCategory>> getCategories() {
        return ResponseEntity.ok(warehouseService.getWarehouseCategories());
    }

    @GetMapping("/racks")
    public ResponseEntity<List<WarehouseRackDTO>> getAllRacks() {
        return ResponseEntity.ok(warehouseService.getAllRacks());
    }

    @PostMapping("/racks")
    public ResponseEntity<WarehouseRackDTO> createRack(@RequestBody WarehouseRackDTO dto) {
        return ResponseEntity.status(201).body(warehouseService.createRack(dto));
    }

    @PutMapping("/racks/{id}/category")
    public ResponseEntity<WarehouseRackDTO> updateRackCategory(
            @PathVariable String id,
            @RequestBody java.util.Map<String, String> body) {
        UUID categoryId = UUID.fromString(body.get("categoryId"));
        return ResponseEntity.ok(warehouseService.updateRackCategory(id, categoryId));
    }

    @GetMapping("/stock")
    public ResponseEntity<List<WarehouseStockDTO>> getAllStock() {
        return ResponseEntity.ok(warehouseService.getAllStock());
    }

    @PostMapping("/stock/adjust")
    public ResponseEntity<WarehouseStockDTO> adjustStock(@Valid @RequestBody WarehouseStockAdjustmentDTO dto) {
        return ResponseEntity.ok(warehouseService.adjustStock(dto));
    }

    @GetMapping("/movements")
    public ResponseEntity<List<WarehouseMovementDTO>> getMovements() {
        return ResponseEntity.ok(warehouseService.getMovements());
    }
}
