package com.Billing_System.controller;

import com.Billing_System.dto.GRNRequestDTO;
import com.Billing_System.dto.GRNResponseDTO;
import com.Billing_System.service.GRNService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for Goods Receipt Note (GRN) operations.
 */
@RestController
@RequestMapping("/api/grn")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class GRNController {

    private final GRNService grnService;

    /**
     * Create a new GRN (DRAFT).
     */
    @PostMapping
    public ResponseEntity<GRNResponseDTO> createGRN(
            @Valid @RequestBody GRNRequestDTO request,
            @RequestHeader(value = "X-User-Id", required = true) UUID userId) {
        
        GRNResponseDTO response = grnService.createGRN(request, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Approve/Finalize a GRN.
     * Triggers stock updates, PO status changes, and VendorProduct mapping.
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<GRNResponseDTO> approveGRN(@PathVariable UUID id) {
        GRNResponseDTO response = grnService.approveGRN(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/finalize")
    public ResponseEntity<GRNResponseDTO> finalizeGRN(@PathVariable UUID id) {
        return approveGRN(id);
    }

    /**
     * Get all Goods Received Notes.
     */
    @GetMapping
    public ResponseEntity<List<GRNResponseDTO>> getAllGRNs() {
        return ResponseEntity.ok(grnService.getAllGRNs());
    }

    /**
     * Get a specific GRN by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<GRNResponseDTO> getGRN(@PathVariable UUID id) {
        return ResponseEntity.ok(grnService.getGRNById(id));
    }

    /**
     * Get all GRNs for a specific Purchase Order.
     */
    @GetMapping("/purchase-order/{poId}")
    public ResponseEntity<List<GRNResponseDTO>> getGRNsByPO(@PathVariable UUID poId) {
        return ResponseEntity.ok(grnService.getGRNsByPO(poId));
    }
}
