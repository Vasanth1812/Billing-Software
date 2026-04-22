package com.Billing_System.controller;

import com.Billing_System.dto.SupplierDTO;
import com.Billing_System.entity.Supplier;
import com.Billing_System.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor

public class SupplierController {

    private final SupplierService supplierService;

    /**
     * GET /api/suppliers
     * List all suppliers
     */
    @GetMapping
    public ResponseEntity<List<Supplier>> getAllSuppliers() {
        return ResponseEntity.ok(supplierService.getAllSuppliers());
    }

    /**
     * GET /api/suppliers/{id}
     * Get a single supplier by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Supplier> getSupplierById(@PathVariable UUID id) {
        return ResponseEntity.ok(supplierService.getSupplierById(id));
    }

    /**
     * POST /api/suppliers
     * Create new supplier
     */
    @PostMapping
    public ResponseEntity<Supplier> createSupplier(@Valid @RequestBody SupplierDTO dto) {
        Supplier created = supplierService.createSupplier(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PUT /api/suppliers/{id}
     * Update supplier details
     */
    @PutMapping("/{id}")
    public ResponseEntity<Supplier> updateSupplier(@PathVariable UUID id,
            @Valid @RequestBody SupplierDTO dto) {
        return ResponseEntity.ok(supplierService.updateSupplier(id, dto));
    }

    /**
     * DELETE /api/suppliers/{id}
     * Delete a supplier
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteSupplier(@PathVariable UUID id) {
        supplierService.deleteSupplier(id);
        return ResponseEntity.ok(Map.of("message", "Supplier deleted successfully"));
    }
}
