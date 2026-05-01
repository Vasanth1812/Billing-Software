package com.Billing_System.controller;

import com.Billing_System.dto.BulkImportResponseDTO;
import com.Billing_System.dto.ProductDTO;
import com.Billing_System.entity.Product;
import com.Billing_System.service.BulkImportService;
import com.Billing_System.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductService productService;
    private final BulkImportService bulkImportService;

    /**
     * GET /api/products
     * List all products with current stock
     */
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    /**
     * GET /api/products/search?q=
     * Search products by name or SKU/barcode
     */
    @GetMapping("/search")
    public ResponseEntity<List<Product>> searchProducts(@RequestParam("q") String query) {
        return ResponseEntity.ok(productService.searchProducts(query));
    }

    /**
     * GET /api/products/barcode/{sku}
     *
     * POS barcode scan endpoint — exact SKU match, returns ONE product instantly.
     * Used when cashier scans a barcode → product auto-added to current order.
     *
     * Example: GET /api/products/barcode/8901030844208
     * → 200 OK with product details (name, mrp, gstRate, currentStock)
     * → 404 if barcode not found / product inactive
     */
    @GetMapping("/barcode/{sku}")
    public ResponseEntity<Product> getProductByBarcode(@PathVariable String sku) {
        return ResponseEntity.ok(productService.getProductBySku(sku));
    }

    /**
     * GET /api/products/low-stock
     * Get all products below minimum stock level
     */
    @GetMapping("/low-stock")
    public ResponseEntity<List<Product>> getLowStockProducts() {
        return ResponseEntity.ok(productService.getLowStockProducts());
    }

    /**
     * GET /api/products/duplicates
     *
     * Returns all products that share the same barcode with at least one other product.
     * These are products that need their barcode corrected after a bulk import.
     *
     * Frontend "Duplicates" button calls this endpoint.
     * The user edits each product (PUT /api/products/{id}) to assign a unique barcode.
     *
     * Response groups products by barcode — all products with the same barcode
     * appear consecutively (sorted by barcode, then name).
     *
     * Example response when "8901030844208" is shared by 2 products:
     * [
     *   { "sku": "8901030844208",       "barcode": "8901030844208", "name": "Amul Butter 500g" },
     *   { "sku": "8901030844208-DUP-2", "barcode": "8901030844208", "name": "Amul Butter 1Kg"  }
     * ]
     */
    @GetMapping("/duplicates")
    public ResponseEntity<List<Product>> getDuplicateBarcodeProducts() {
        return ResponseEntity.ok(productService.getDuplicateBarcodeProducts());
    }

    /**
     * GET /api/products/{id}
     * Get a single product by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    /**
     * POST /api/products
     * Create new product (currentStock auto-initialised to 0)
     */
    @PostMapping
    public ResponseEntity<Product> createProduct(@Valid @RequestBody ProductDTO dto) {
        Product created = productService.createProduct(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PUT /api/products/{id}
     * Update product details (including currentStock)
     */
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable UUID id,
            @Valid @RequestBody ProductDTO dto) {
        return ResponseEntity.ok(productService.updateProduct(id, dto));
    }

    /**
     * DELETE /api/products/{id}
    /**
     * DELETE /api/products/{id}
     * Soft delete product (isActive = false)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(Map.of("message", "Product deleted successfully"));
    }

    /**
     * POST /api/products/bulk-import
     *
     * BillPro Bulk Upload — imports products from a .xlsx file in batches of 500.
     *
     * QUERY PARAMETERS:
     *   autoCreateSuppliers=false (default) — STRICT mode:
     *     Supplier name in Column M must already exist in DB.
     *     Recommended for daily use. Rejects rows with unknown suppliers.
     *
     *   autoCreateSuppliers=true — AUTO mode:
     *     If supplier not found, creates it automatically with just the name.
     *     Use this during INITIAL SETUP when importing a large catalogue.
     *     Admin must fill in supplier details (phone, GSTIN) afterwards.
     *
     * EXAMPLES:
     *   POST /api/products/bulk-import                          → strict
     *   POST /api/products/bulk-import?autoCreateSuppliers=true → auto-create
     */
    @PostMapping(value = "/bulk-import", consumes = "multipart/form-data")
    public ResponseEntity<BulkImportResponseDTO> bulkImport(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "autoCreateSuppliers", defaultValue = "false") boolean autoCreateSuppliers) {
        BulkImportResponseDTO result = bulkImportService.importProductsFromXlsx(file, autoCreateSuppliers);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
