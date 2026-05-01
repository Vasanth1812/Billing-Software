package com.Billing_System.service;

import com.Billing_System.dto.BulkImportResponseDTO;
import com.Billing_System.entity.Category;
import com.Billing_System.entity.Product;
import com.Billing_System.entity.Supplier;
import com.Billing_System.repository.CategoryRepository;
import com.Billing_System.repository.ProductRepository;
import com.Billing_System.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

/**
 * BulkImportService — Imports products from a BillPro-format .xlsx file.
 *
 * ═══ BillPro XLSX Format (Products Sheet) ═══
 * Row 1: Legend banner (colour-coded) — DO NOT EDIT
 * Row 2: Column headers (dark blue)   — DO NOT EDIT
 * Row 3: Sample data (blue italic)    — DELETE before upload
 * Row 4+: Actual data
 *
 * Max 5,000 rows per file.
 *
 * Column mapping (0-indexed):
 *  0  - Product Name        (REQUIRED)
 *  1  - SKU / Barcode       (REQUIRED — barcode value; can be duplicate across products)
 *  2  - Category            (REQUIRED)
 *  3  - Unit of Measure     (REQUIRED)
 *  4  - Purchase Rate       (REQUIRED)
 *  5  - MRP / Selling Price (REQUIRED)
 *  6  - GST %               (REQUIRED: 0/5/12/18/28)
 *  7  - HSN Code            (REQUIRED)
 *  8  - Opening Stock Qty   (optional → currentStock)
 *  9  - Min Stock Level     (optional)
 * 10  - Product Description (optional)
 * 11  - Brand               (optional)
 * 12  - Supplier Name       (REQUIRED — strict lookup, must exist in suppliers table)
 * 13  - Expiry/Shelf Life   (optional, appended to description)
 * 14  - Active (YES/NO)     (optional, default: YES)
 *
 * DUPLICATE BARCODE BEHAVIOUR:
 *   - Multiple rows with the same barcode are ALL imported (client requirement).
 *   - 1st occurrence → SKU = barcode        (e.g. "8901030844208")
 *   - 2nd occurrence → SKU = barcode-DUP-2  (e.g. "8901030844208-DUP-2")
 *   - 3rd occurrence → SKU = barcode-DUP-3  etc.
 *   - Frontend "Duplicates" button calls GET /api/products/duplicates to list them.
 *   - User then edits each product and sets a unique barcode.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BulkImportService {

    private static final int BATCH_SIZE    = 500;
    private static final int MAX_ROWS      = 5000;
    private static final int DATA_START_ROW = 3; // 0-indexed: Row 4 in Excel = index 3

    // Column indices (0-based)
    private static final int COL_NAME          = 0;
    private static final int COL_SKU           = 1;  // barcode value in XLSX
    private static final int COL_CATEGORY      = 2;
    private static final int COL_UNIT          = 3;
    private static final int COL_PURCHASE_RATE = 4;
    private static final int COL_MRP           = 5;
    private static final int COL_GST_RATE      = 6;
    private static final int COL_HSN_CODE      = 7;
    private static final int COL_OPENING_STOCK = 8;
    private static final int COL_MIN_STOCK     = 9;
    private static final int COL_DESCRIPTION   = 10;
    private static final int COL_BRAND         = 11;
    private static final int COL_SUPPLIER_NAME = 12;
    private static final int COL_EXPIRY        = 13;
    private static final int COL_ACTIVE        = 14;

    private static final Set<BigDecimal> VALID_GST_RATES = Set.of(
            BigDecimal.ZERO,
            new BigDecimal("5"),
            new BigDecimal("12"),
            new BigDecimal("18"),
            new BigDecimal("28")
    );

    private final ProductRepository  productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;

    /**
     * Main entry point — parse XLSX, batch insert, return import report.
     *
     * @param file               the uploaded .xlsx file
     * @param autoCreateSuppliers when TRUE  → if supplier name not found in DB, auto-create
     *                                          it with just the name. Admin fills details later.
     *                            when FALSE → strict mode: supplier must already exist in DB
     *                                          (recommended for production use)
     */
    @Transactional
    public BulkImportResponseDTO importProductsFromXlsx(MultipartFile file,
                                                         boolean autoCreateSuppliers) {
        validateFile(file);

        List<String>  skippedSkus = new ArrayList<>();
        List<String>  errors      = new ArrayList<>();
        List<Product> batch       = new ArrayList<>(BATCH_SIZE);
        int successCount          = 0;
        int skippedCount          = 0;
        int totalRows             = 0;
        int duplicateBarcodeCount = 0;

        // Cache categories in memory — avoids DB hit per row
        Map<String, Category> categoryCache = new HashMap<>();

        // Pre-load ALL supplier names into memory for fast strict lookup
        Map<String, Supplier> supplierCache = new HashMap<>();
        supplierRepository.findAll().forEach(s ->
                supplierCache.put(s.getName().toLowerCase(), s));

        // Pre-load ALL existing SKUs — O(1) check to avoid inserting same product twice
        Set<String> existingSkus = new HashSet<>(productRepository.findAllSkus());

        // Pre-load ALL existing barcodes — to flag cross-file duplicates
        Set<String> existingBarcodes = new HashSet<>(productRepository.findAllBarcodes());

        // Track barcode occurrences WITHIN this file
        // Key: barcode value → how many times seen so far in this upload
        Map<String, Integer> barcodeCountInFile = new HashMap<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {

            Sheet sheet      = workbook.getSheetAt(0);
            int   lastRowNum = sheet.getLastRowNum();

            totalRows = Math.max(0, lastRowNum - DATA_START_ROW + 1);

            if (totalRows > MAX_ROWS) {
                throw new IllegalArgumentException(
                        "File has " + totalRows + " rows. BillPro max is "
                        + MAX_ROWS + " rows per upload. Split into multiple files.");
            }

            log.info("Bulk import started — {} data rows (rows 4–{})", totalRows, lastRowNum + 1);

            for (int rowIdx = DATA_START_ROW; rowIdx <= lastRowNum; rowIdx++) {
                Row row        = sheet.getRow(rowIdx);
                int excelRowNum = rowIdx + 1; // 1-indexed for user messages

                // Skip completely empty rows
                if (row == null || isRowEmpty(row)) {
                    totalRows--;
                    continue;
                }

                try {
                    String rawBarcode = getCellString(row, COL_SKU);

                    if (rawBarcode.isBlank()) {
                        errors.add("Row " + excelRowNum + ": SKU / Barcode (Column B) is required");
                        continue;
                    }

                    // ── Duplicate Barcode Handling ────────────────────────────────────────
                    // Count occurrences of this barcode in THIS upload file
                    int occurrence = barcodeCountInFile.merge(rawBarcode, 1, Integer::sum);

                    // Generate a unique SKU:
                    //   1st occurrence → SKU = barcode         ("8901030844208")
                    //   2nd occurrence → SKU = barcode-DUP-2   ("8901030844208-DUP-2")
                    //   3rd occurrence → SKU = barcode-DUP-3   ("8901030844208-DUP-3")
                    String sku = (occurrence == 1)
                            ? rawBarcode
                            : rawBarcode + "-DUP-" + occurrence;

                    // Flag if this barcode is a duplicate (within file or against DB)
                    if (occurrence > 1 || existingBarcodes.contains(rawBarcode)) {
                        duplicateBarcodeCount++;
                        log.debug("Row {} — duplicate barcode '{}', assigned SKU '{}'",
                                excelRowNum, rawBarcode, sku);
                    }

                    // Skip if this exact SKU already exists (re-upload protection)
                    if (existingSkus.contains(sku)) {
                        skippedSkus.add(sku);
                        skippedCount++;
                        log.debug("Row {} — SKU '{}' already in DB, skipped", excelRowNum, sku);
                        continue;
                    }

                    Product product = mapRowToProduct(row, excelRowNum, sku, rawBarcode,
                                                      categoryCache, supplierCache,
                                                      autoCreateSuppliers);
                    batch.add(product);
                    existingSkus.add(sku);
                    existingBarcodes.add(rawBarcode);

                    // Flush batch to DB when full
                    if (batch.size() == BATCH_SIZE) {
                        productRepository.saveAll(batch);
                        successCount += batch.size();
                        log.info("Batch saved — {} products inserted so far", successCount);
                        batch.clear();
                    }

                } catch (Exception e) {
                    errors.add("Row " + excelRowNum + ": " + e.getMessage());
                    log.warn("Row {} failed: {}", excelRowNum, e.getMessage());
                }
            }

            // Save the final partial batch (remainder < 500)
            if (!batch.isEmpty()) {
                productRepository.saveAll(batch);
                successCount += batch.size();
            }

        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read XLSX file: " + e.getMessage());
        }

        log.info("Bulk import complete — success={}, skipped={}, duplicateBarcodes={}, failed={}",
                successCount, skippedCount, duplicateBarcodeCount, errors.size());

        return BulkImportResponseDTO.builder()
                .totalRows(totalRows)
                .successCount(successCount)
                .skippedCount(skippedCount)
                .failedCount(errors.size())
                .duplicateBarcodeCount(duplicateBarcodeCount)
                .skippedSkus(skippedSkus)
                .errors(errors)
                .build();
    }

    // ─── Private Helpers ────────────────────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Upload file is empty");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new IllegalArgumentException(
                    "Only .xlsx files are accepted. Received: " + filename
                    + ". Do NOT upload .csv, .xls, or .ods files.");
        }
    }

    /**
     * Maps one XLSX row to a Product entity.
     *
     * @param sku        unique SKU (may have -DUP-N suffix for duplicate barcodes)
     * @param rawBarcode original barcode value from the XLSX (stored in product.barcode)
     */
    private Product mapRowToProduct(Row row, int excelRowNum,
                                     String sku, String rawBarcode,
                                     Map<String, Category> categoryCache,
                                     Map<String, Supplier> supplierCache,
                                     boolean autoCreateSuppliers) {
        String name = getCellString(row, COL_NAME);
        if (name.isBlank()) throw new IllegalArgumentException("Product Name (Column A) is required");

        // Category — auto-create if not found in DB
        String categoryName = getCellString(row, COL_CATEGORY);
        if (categoryName.isBlank()) throw new IllegalArgumentException("Category (Column C) is required");
        Category category = categoryCache.computeIfAbsent(categoryName, this::findOrCreateCategory);

        String unit = getCellString(row, COL_UNIT);
        if (unit.isBlank()) throw new IllegalArgumentException("Unit of Measure (Column D) is required");

        BigDecimal purchaseRate = parseBigDecimalRequired(row, COL_PURCHASE_RATE, "Purchase Rate (Column E)", excelRowNum);
        BigDecimal mrp          = parseBigDecimalRequired(row, COL_MRP, "MRP/Selling Price (Column F)", excelRowNum);

        if (mrp.compareTo(purchaseRate) < 0) {
            throw new IllegalArgumentException(
                    "MRP (" + mrp + ") must be ≥ Purchase Rate (" + purchaseRate + ")");
        }

        BigDecimal gstRate    = parseBigDecimalRequired(row, COL_GST_RATE, "GST % (Column G)", excelRowNum);
        BigDecimal gstRounded = gstRate.stripTrailingZeros().setScale(0, java.math.RoundingMode.UNNECESSARY);
        if (!VALID_GST_RATES.contains(gstRate) && !VALID_GST_RATES.contains(gstRounded)) {
            throw new IllegalArgumentException(
                    "Invalid GST rate: " + gstRate + ". Must be one of: 0, 5, 12, 18, 28");
        }

        String hsnCode = getCellString(row, COL_HSN_CODE);
        if (hsnCode.isBlank()) throw new IllegalArgumentException("HSN Code (Column H) is required");

        // ── Supplier Lookup ───────────────────────────────────────────────────
        String supplierName = getCellString(row, COL_SUPPLIER_NAME);
        if (supplierName.isBlank()) {
            throw new IllegalArgumentException(
                    "Supplier Name (Column M) is required for traceability. "
                    + "Add the supplier in Suppliers module first, then re-upload.");
        }

        Supplier primarySupplier = supplierCache.get(supplierName.toLowerCase());

        if (primarySupplier == null) {
            if (autoCreateSuppliers) {
                // Auto-create with name only — admin fills details later
                log.info("Auto-creating supplier: '{}'", supplierName);
                primarySupplier = supplierRepository.save(
                        Supplier.builder().name(supplierName).build());
                // Cache it so we don't create duplicates for the same name in this file
                supplierCache.put(supplierName.toLowerCase(), primarySupplier);
            } else {
                // Strict mode — reject the row
                throw new IllegalArgumentException(
                        "Supplier '" + supplierName + "' (Column M) not found in system. "
                        + "Go to Suppliers → Add Supplier, then re-upload. "
                        + "Or use ?autoCreateSuppliers=true to auto-create missing suppliers.");
            }
        }

        // Optional fields
        BigDecimal openingStock = parseBigDecimalOptional(row, COL_OPENING_STOCK);
        BigDecimal minStock     = parseBigDecimalOptional(row, COL_MIN_STOCK);
        String description      = getCellString(row, COL_DESCRIPTION);
        String brand            = getCellString(row, COL_BRAND);
        String expiry           = getCellString(row, COL_EXPIRY);

        if (!expiry.isBlank()) {
            description = description.isBlank()
                    ? "Shelf Life: " + expiry
                    : description + " | Shelf Life: " + expiry;
        }

        String  activeCell = getCellString(row, COL_ACTIVE).toUpperCase().trim();
        boolean isActive   = !activeCell.equals("NO");

        return Product.builder()
                .name(name)
                .sku(sku)                               // unique (may have -DUP-N suffix)
                .barcode(rawBarcode)                    // original barcode (may be duplicate)
                .category(category)
                .primarySupplier(primarySupplier)
                .unit(unit)
                .purchaseRate(purchaseRate)
                .mrp(mrp)
                .sellingPrice(mrp)
                .gstRate(gstRate)
                .hsnCode(hsnCode)
                .currentStock(openingStock != null ? openingStock : BigDecimal.ZERO)
                .minStock(minStock != null ? minStock : BigDecimal.ZERO)
                .description(description.isBlank() ? null : description)
                .brand(brand.isBlank() ? null : brand)
                .isActive(isActive)
                .build();
    }

    /** Find category by name (case-insensitive) OR create it automatically */
    private Category findOrCreateCategory(String name) {
        return categoryRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> {
                    log.info("Auto-creating new category: '{}'", name);
                    return categoryRepository.save(Category.builder().name(name).build());
                });
    }

    /** Read cell as String — handles Numeric, String, Boolean, Formula, Blank cells */
    private String getCellString(Row row, int colIdx) {
        if (row == null) return "";
        Cell cell = row.getCell(colIdx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return "";

        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double val = cell.getNumericCellValue();
                yield (val == Math.floor(val)) ? String.valueOf((long) val) : String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try { yield String.valueOf(cell.getNumericCellValue()); }
                catch (Exception e) { yield cell.getStringCellValue().trim(); }
            }
            default -> "";
        };
    }

    /** Parse required BigDecimal — throws clear error if missing or not numeric */
    private BigDecimal parseBigDecimalRequired(Row row, int colIdx, String fieldName, int rowNum) {
        String val = getCellString(row, colIdx);
        if (val.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required but empty");
        }
        try {
            return new BigDecimal(val);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    fieldName + " has invalid value '" + val
                    + "'. Use plain numbers only — no ₹, commas, or text");
        }
    }

    /** Parse optional BigDecimal — returns null if blank */
    private BigDecimal parseBigDecimalOptional(Row row, int colIdx) {
        String val = getCellString(row, colIdx);
        if (val.isBlank()) return null;
        try {
            return new BigDecimal(val);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Check if an entire row has no meaningful data */
    private boolean isRowEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                if (!getCellString(row, c).isBlank()) return false;
            }
        }
        return true;
    }
}
