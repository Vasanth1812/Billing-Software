package com.Billing_System.controller;

import com.Billing_System.dto.HeldSaleDTO;
import com.Billing_System.service.HeldSaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sales/hold")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class HeldSaleController {

    private final HeldSaleService heldSaleService;

    /**
     * POST /api/sales/hold
     * Save current cart for later recall
     */
    @PostMapping
    public ResponseEntity<HeldSaleDTO> holdSale(@RequestBody HeldSaleDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(heldSaleService.holdSale(dto));
    }

    /**
     * GET /api/sales/hold
     * Retrieve all active holds (optionally filtered by user)
     */
    @GetMapping
    public ResponseEntity<List<HeldSaleDTO>> getHeldSales(@RequestParam(required = false) UUID userId) {
        return ResponseEntity.ok(heldSaleService.getHeldSales(userId));
    }

    /**
     * DELETE /api/sales/hold/{id}
     * Clear a specific hold after recall
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHeldSale(@PathVariable UUID id) {
        heldSaleService.deleteHeldSale(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /api/sales/hold
     * Clear all held bills
     */
    @DeleteMapping
    public ResponseEntity<Void> clearAllHeldSales() {
        heldSaleService.clearAllHeldSales();
        return ResponseEntity.noContent().build();
    }
}
