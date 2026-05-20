package com.Billing_System.vendor.controller;

import com.Billing_System.vendor.dto.VendorBulkImportResponseDTO;
import com.Billing_System.vendor.dto.VendorProductDTO;
import com.Billing_System.vendor.entity.VendorProduct;
import com.Billing_System.vendor.repository.VendorProductRepository;
import com.Billing_System.vendor.repository.VendorRepository;
import com.Billing_System.vendor.service.VendorBulkImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * VendorProductController — Vendor Product Catalog REST API
 *
 * ── Bulk Upload
 * ───────────────────────────────────────────────────────────────
 * GET /api/vendor-products/template Download Excel template
 * POST /api/vendor-products/bulk-upload Upload vendor product Excel
 *
 * ── Vendor Product CRUD
 * ───────────────────────────────────────────────────────
 * GET /api/vendors/{vendorId}/products All products for a vendor
 * GET /api/vendor-products/{id} Single product detail
 * POST /api/vendor-products/{id}/map-product Map to store product (for GRN)
 * PUT /api/vendor-products/{id}/deactivate Deactivate a product
 */
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class VendorProductController {

        private final VendorBulkImportService bulkImportService;
        private final VendorProductRepository productRepository;
        private final VendorRepository vendorRepository;

        // ── Template Download ─────────────────────────────────────────────────────

        /**
         * GET /api/vendor-products/template
         * Download the blank Excel template for vendor product bulk upload.
         */
        @GetMapping("/api/vendor-products/template")
        public ResponseEntity<byte[]> downloadTemplate() {
                byte[] template = bulkImportService.generateTemplate();
                return ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=\"vendor_products_template.xlsx\"")
                                .contentType(MediaType.parseMediaType(
                                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                                .body(template);
        }

        // ── Bulk Upload ───────────────────────────────────────────────────────────

        /**
         * POST /api/vendor-products/bulk-upload
         * Upload an Excel file with vendor product data.
         *
         * Form-data key: file (multipart/form-data)
         *
         * Rules:
         * - Vendor Code must match an existing ACTIVE vendor
         * - Vendor SKU must be unique per vendor (duplicates → UPDATE, not error)
         * - Max 5000 rows per file
         */
        @PostMapping(value = "/api/vendor-products/bulk-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<VendorBulkImportResponseDTO> bulkUpload(
                        @RequestParam("file") MultipartFile file) {
                VendorBulkImportResponseDTO result = bulkImportService.importFromXlsx(file);
                HttpStatus status = "FAILED".equals(result.getStatus())
                                ? HttpStatus.UNPROCESSABLE_ENTITY
                                : HttpStatus.OK;
                return ResponseEntity.status(status).body(result);
        }

        // ── Vendor Product Queries ────────────────────────────────────────────────

        /**
         * GET /api/vendors/{vendorId}/products
         * List all products in a vendor's catalog.
         * Optional: ?search=milk to filter by name/SKU/brand
         */
        @GetMapping("/api/vendors/{vendorId}/products")
        @Transactional(readOnly = true)
        public ResponseEntity<List<VendorProductDTO>> getVendorProducts(
                        @PathVariable UUID vendorId,
                        @RequestParam(required = false) String search) {

                List<VendorProduct> products = (search != null && !search.isBlank())
                                ? productRepository.searchByVendor(vendorId, search.trim())
                                : productRepository.findByVendorIdAndIsActiveTrueOrderByProductNameAsc(vendorId);

                return ResponseEntity.ok(products.stream()
                                .map(this::toDTO)
                                .collect(Collectors.toList()));
        }

        /**
         * GET /api/vendor-products/{id}
         * Single vendor product detail.
         */
        @GetMapping("/api/vendor-products/{id}")
        @Transactional(readOnly = true)
        public ResponseEntity<VendorProductDTO> getById(@PathVariable UUID id) {
                VendorProduct product = productRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException("Vendor product not found: " + id));
                return ResponseEntity.ok(toDTO(product));
        }

        /**
         * POST /api/vendor-products/{id}/map-product
         * Link this vendor product to the store's product table.
         * Used during GRN to match vendor item → store barcode.
         *
         * Body: { "productId": "store-product-uuid" }
         */
        @PostMapping("/api/vendor-products/{id}/map-product")
        @Transactional
        public ResponseEntity<VendorProductDTO> mapToStoreProduct(
                        @PathVariable UUID id,
                        @RequestBody Map<String, String> body) {

                String productIdStr = body.get("productId");
                if (productIdStr == null || productIdStr.isBlank()) {
                        throw new IllegalArgumentException("productId is required in request body.");
                }

                UUID productId;
                try {
                        productId = UUID.fromString(productIdStr);
                } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Invalid productId format: " + productIdStr);
                }

                VendorProduct vp = productRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException("Vendor product not found: " + id));

                vp.setMappedProductId(productId);
                vp.setUpdatedAt(LocalDateTime.now());
                productRepository.save(vp);

                return ResponseEntity.ok(toDTO(vp));
        }

        /**
         * GET /api/vendor-products
         * List all active vendor products across all vendors (for global product screen).
         */
        @GetMapping("/api/vendor-products")
        @Transactional(readOnly = true)
        public ResponseEntity<List<VendorProductDTO>> getAllProductsGlobal() {
                List<VendorProduct> products = productRepository.findAll();
                return ResponseEntity.ok(products.stream()
                                .map(this::toDTO)
                                .collect(Collectors.toList()));
        }

        /**
         * PUT /api/vendor-products/{id}/deactivate
         * Deactivate a vendor product (soft delete — keeps history).
         */
        @PutMapping("/api/vendor-products/{id}/deactivate")
        public ResponseEntity<Map<String, String>> deactivate(@PathVariable UUID id) {
                VendorProduct vp = productRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException("Vendor product not found: " + id));
                vp.setActive(false);
                vp.setUpdatedAt(LocalDateTime.now());
                productRepository.save(vp);
                return ResponseEntity.ok(Map.of("message", "Vendor product deactivated successfully"));
        }

        /**
         * PUT /api/vendor-products/{id}
         * Update an existing vendor product's details.
         */
        @PutMapping("/api/vendor-products/{id}")
        @Transactional
        public ResponseEntity<VendorProductDTO> updateVendorProduct(
                        @PathVariable UUID id,
                        @RequestBody com.Billing_System.vendor.dto.VendorProductRequestDTO dto) {

                VendorProduct vp = productRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException("Vendor product not found: " + id));

                vp.setProductName(dto.getProductName());
                vp.setVendorSku(dto.getVendorSku());
                vp.setPurchasePrice(dto.getPurchasePrice());
                vp.setUnitOfMeasure(dto.getUnitOfMeasure());
                vp.setPackSize(dto.getPackSize());
                vp.setGstRate(dto.getGstRate());
                vp.setHsnCode(dto.getHsnCode());
                vp.setBrand(dto.getBrand());
                vp.setCategory(dto.getCategory());
                vp.setMinOrderQty(dto.getMinOrderQty());
                vp.setDescription(dto.getDescription());
                vp.setBatchNumber(dto.getBatchNumber());
                vp.setExpiryDate(dto.getExpiryDate());
                if (dto.getMappedProductId() != null) {
                        vp.setMappedProductId(dto.getMappedProductId());
                }
                vp.setUpdatedAt(LocalDateTime.now());

                productRepository.save(vp);
                return ResponseEntity.ok(toDTO(vp));
        }

        /**
         * DELETE /api/vendor-products/{id}
         * Delete/deactivate a vendor product.
         */
        @DeleteMapping("/api/vendor-products/{id}")
        @Transactional
        public ResponseEntity<Map<String, String>> deleteVendorProduct(@PathVariable UUID id) {
                VendorProduct vp = productRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException("Vendor product not found: " + id));
                productRepository.delete(vp);
                return ResponseEntity.ok(Map.of("message", "Vendor product deleted successfully"));
        }

        // ── Mapping Helper ────────────────────────────────────────────────────────

        private VendorProductDTO toDTO(VendorProduct vp) {
                return VendorProductDTO.builder()
                                .id(vp.getId())
                                .vendorCode(vp.getVendor() != null ? vp.getVendor().getVendorCode() : null)
                                .vendorLegalName(vp.getVendor() != null ? vp.getVendor().getLegalName() : null)
                                .productName(vp.getProductName())
                                .vendorSku(vp.getVendorSku())
                                .purchasePrice(vp.getPurchasePrice())
                                .unitOfMeasure(vp.getUnitOfMeasure())
                                .packSize(vp.getPackSize())
                                .gstRate(vp.getGstRate())
                                .hsnCode(vp.getHsnCode())
                                .brand(vp.getBrand())
                                .category(vp.getCategory())
                                .minOrderQty(vp.getMinOrderQty())
                                .description(vp.getDescription())
                                .batchNumber(vp.getBatchNumber())
                                .expiryDate(vp.getExpiryDate())
                                .mappedProductId(vp.getMappedProductId())
                                .isActive(vp.isActive())
                                .createdAt(vp.getCreatedAt())
                                .updatedAt(vp.getUpdatedAt())
                                .build();
        }
}
