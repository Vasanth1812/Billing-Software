package com.Billing_System.vendor.controller;

import com.Billing_System.vendor.service.ComplianceSchedulerService;
import com.Billing_System.vendor.service.VendorMasterBulkImportService;
import com.Billing_System.vendor.service.VendorService;
import com.Billing_System.vendor.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * VendorController — Vendor lifecycle management REST API
 *
 * Base path: /api/vendors
 *
 * ── Vendor CRUD ──────────────────────────────────────────────────────────────
 * GET    /api/vendors                              List all vendors (with filters)
 * POST   /api/vendors                              Create new vendor (starts onboarding)
 * GET    /api/vendors/{id}                         Get vendor detail with all sub-resources
 * PUT    /api/vendors/{id}                         Update vendor master info
 * DELETE /api/vendors/{id}                         Soft delete vendor
 *
 * ── Onboarding Workflow ───────────────────────────────────────────────────────
 * POST   /api/vendors/{id}/onboarding/approve      Advance to next stage
 * POST   /api/vendors/{id}/onboarding/reject        Reject at current stage
 * PATCH  /api/vendors/{id}/block                    Block vendor (compliance/fraud)
 * PATCH  /api/vendors/{id}/unblock                  Unblock vendor
 *
 * ── Locations ────────────────────────────────────────────────────────────────
 * POST   /api/vendors/{id}/locations               Add new location
 * DELETE /api/vendors/locations/{locationId}        Delete location
 *
 * ── Bank Accounts ────────────────────────────────────────────────────────────
 * POST   /api/vendors/{id}/bank-accounts           Add bank account
 * DELETE /api/vendors/bank-accounts/{accountId}     Delete bank account
 *
 * ── Documents ────────────────────────────────────────────────────────────────
 * POST   /api/vendors/{id}/documents               Upload/add document
 * POST   /api/vendors/documents/{docId}/approve     Approve document
 * POST   /api/vendors/documents/{docId}/reject      Reject document with reason
 * DELETE /api/vendors/documents/{docId}             Delete document
 *
 * ── Compliance ───────────────────────────────────────────────────────────────
 * POST   /api/vendors/compliance/scan              Trigger manual compliance scan
 */
@RestController
@RequestMapping("/api/vendors")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class VendorController {

    private final VendorService                vendorService;
    private final ComplianceSchedulerService    complianceScheduler;
    private final VendorMasterBulkImportService masterBulkImportService;

    // ─── Vendor Master Bulk Upload ────────────────────────────────────────────

    /**
     * GET /api/vendors/bulk-upload/template
     * Download blank Excel template for vendor master bulk upload.
     */
    @GetMapping("/bulk-upload/template")
    public ResponseEntity<byte[]> downloadMasterTemplate() {
        byte[] template = masterBulkImportService.generateTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"vendor_master_template.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(template);
    }

    /**
     * POST /api/vendors/bulk-upload
     * Upload Excel with vendor master data.
     * Auto-generates VND-000001, VND-000002... vendor codes.
     * Upserts by GSTIN — updates existing vendor if GSTIN matches.
     */
    @PostMapping(value = "/bulk-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VendorBulkImportResponseDTO> masterBulkUpload(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        VendorBulkImportResponseDTO result = masterBulkImportService.importFromXlsx(file, userId);
        HttpStatus status = "FAILED".equals(result.getStatus())
                ? HttpStatus.UNPROCESSABLE_ENTITY : HttpStatus.OK;
        return ResponseEntity.status(status).body(result);
    }

    // ─── Vendor CRUD ─────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<VendorResponseDTO>> getAllVendors(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String complianceStatus,
            @RequestParam(required = false) String kycStatus) {
        return ResponseEntity.ok(vendorService.getAllVendors(search, complianceStatus, kycStatus));
    }

    @PostMapping
    public ResponseEntity<VendorResponseDTO> createVendor(
            @Valid @RequestBody VendorRequestDTO dto,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        VendorResponseDTO created = vendorService.createVendor(dto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VendorResponseDTO> getVendorById(@PathVariable UUID id) {
        return ResponseEntity.ok(vendorService.getVendorById(id));
    }

    /** Phase 2: All POs raised for this vendor */
    @GetMapping("/{id}/purchase-orders")
    public ResponseEntity<List<VendorPurchaseHistoryDTO>> getVendorPurchaseOrders(@PathVariable UUID id) {
        return ResponseEntity.ok(vendorService.getVendorPurchaseOrders(id));
    }

    /** Phase 2: Dashboard stats for this vendor */
    @GetMapping("/{id}/stats")
    public ResponseEntity<VendorStatsDTO> getVendorStats(@PathVariable UUID id) {
        return ResponseEntity.ok(vendorService.getVendorStats(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VendorResponseDTO> updateVendor(
            @PathVariable UUID id,
            @Valid @RequestBody VendorRequestDTO dto) {
        return ResponseEntity.ok(vendorService.updateVendor(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteVendor(@PathVariable UUID id) {
        vendorService.deleteVendor(id);
        return ResponseEntity.ok(Map.of("message", "Vendor deleted successfully"));
    }

    // ─── Onboarding Workflow ─────────────────────────────────────────────────

    @PostMapping("/{id}/onboarding/approve")
    public ResponseEntity<VendorResponseDTO> approveOnboarding(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        String comments = body != null ? body.get("comments") : null;
        return ResponseEntity.ok(vendorService.approveOnboarding(id, comments));
    }

    @PostMapping("/{id}/onboarding/reject")
    public ResponseEntity<VendorResponseDTO> rejectVendor(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(vendorService.rejectVendor(id, reason));
    }

    @PatchMapping("/{id}/block")
    public ResponseEntity<VendorResponseDTO> blockVendor(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(vendorService.blockVendor(id, reason));
    }

    @PatchMapping("/{id}/unblock")
    public ResponseEntity<VendorResponseDTO> unblockVendor(@PathVariable UUID id) {
        return ResponseEntity.ok(vendorService.unblockVendor(id));
    }

    // ─── Locations ───────────────────────────────────────────────────────────

    @PostMapping("/{id}/locations")
    public ResponseEntity<VendorResponseDTO.LocationDTO> addLocation(
            @PathVariable UUID id,
            @Valid @RequestBody VendorLocationDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vendorService.addLocation(id, dto));
    }

    @DeleteMapping("/locations/{locationId}")
    public ResponseEntity<Map<String, String>> deleteLocation(@PathVariable UUID locationId) {
        vendorService.deleteLocation(locationId);
        return ResponseEntity.ok(Map.of("message", "Location deleted successfully"));
    }

    // ─── Bank Accounts ───────────────────────────────────────────────────────

    @PostMapping("/{id}/bank-accounts")
    public ResponseEntity<VendorResponseDTO.BankAccountDTO> addBankAccount(
            @PathVariable UUID id,
            @Valid @RequestBody VendorBankAccountDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vendorService.addBankAccount(id, dto));
    }

    @DeleteMapping("/bank-accounts/{accountId}")
    public ResponseEntity<Map<String, String>> deleteBankAccount(@PathVariable UUID accountId) {
        vendorService.deleteBankAccount(accountId);
        return ResponseEntity.ok(Map.of("message", "Bank account deleted successfully"));
    }

    // ─── Documents ───────────────────────────────────────────────────────────

    @PostMapping("/{id}/documents")
    public ResponseEntity<VendorResponseDTO.DocumentDTO> addDocument(
            @PathVariable UUID id,
            @Valid @RequestBody VendorDocumentDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vendorService.addDocument(id, dto));
    }

    @PostMapping("/documents/{docId}/approve")
    public ResponseEntity<VendorResponseDTO.DocumentDTO> approveDocument(
            @PathVariable UUID docId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        return ResponseEntity.ok(vendorService.approveDocument(docId, userId));
    }

    @PostMapping("/documents/{docId}/reject")
    public ResponseEntity<VendorResponseDTO.DocumentDTO> rejectDocument(
            @PathVariable UUID docId,
            @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "No reason provided");
        return ResponseEntity.ok(vendorService.rejectDocument(docId, reason));
    }

    @DeleteMapping("/documents/{docId}")
    public ResponseEntity<Map<String, String>> deleteDocument(@PathVariable UUID docId) {
        vendorService.deleteDocument(docId);
        return ResponseEntity.ok(Map.of("message", "Document deleted successfully"));
    }

    // ─── Compliance ──────────────────────────────────────────────────────────

    /** Admin endpoint — trigger compliance scan immediately (useful for testing) */
    @PostMapping("/compliance/scan")
    public ResponseEntity<ComplianceSchedulerService.ComplianceScanResultDTO> triggerComplianceScan() {
        return ResponseEntity.ok(complianceScheduler.runManualScan());
    }
}
