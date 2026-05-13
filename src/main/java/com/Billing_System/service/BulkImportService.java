package com.Billing_System.service;

import com.Billing_System.dto.BulkImportResponseDTO;
import com.Billing_System.entity.BulkUpload;
import com.Billing_System.entity.BulkUploadRow;
import com.Billing_System.entity.BulkUploadTemplate;
import com.Billing_System.entity.Category;
import com.Billing_System.entity.Product;
import com.Billing_System.entity.Supplier;
import com.Billing_System.entity.StockLedger;
import com.Billing_System.repository.BulkUploadRepository;
import com.Billing_System.repository.BulkUploadRowRepository;
import com.Billing_System.repository.BulkUploadTemplateRepository;
import com.Billing_System.repository.CategoryRepository;
import com.Billing_System.repository.ProductRepository;
import com.Billing_System.repository.StockLedgerRepository;
import com.Billing_System.repository.SupplierRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.MessageDigest;
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

    private static final String[] TEMPLATE_HEADERS = {
            "Product Name",
            "SKU / Barcode",
            "Category",
            "Unit of Measure",
            "Purchase Rate (\u20b9)",
            "MRP / Selling Price",
            "GST %",
            "HSN Code",
            "Opening Stock Qty",
            "Min Stock Level",
            "Product Description",
            "Brand",
            "Supplier Name",
            "Expiry / Shelf Life",
            "Active (YES/NO)"
    };

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
    private final BulkUploadRepository bulkUploadRepository;
    private final BulkUploadRowRepository bulkUploadRowRepository;
    private final BulkUploadTemplateRepository bulkUploadTemplateRepository;
    private final StockLedgerRepository stockLedgerRepository;
    private final ObjectMapper objectMapper;

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

        String fileHash = calculateFileHash(file);
        if (bulkUploadRepository.existsByFileHashAndStatusIn(fileHash, List.of("SUCCESS", "PARTIAL"))) {
            throw new IllegalArgumentException("Duplicate Upload: This exact file has already been processed successfully. If you modified it, please provide a unique reference number or delete the old upload first.");
        }

        BulkUpload upload = bulkUploadRepository.save(BulkUpload.builder()
                .fileName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "bulk-upload.xlsx")
                .status("PROCESSING")
                .autoCreateSuppliers(autoCreateSuppliers)
                .fileHash(fileHash)
                .build());

        List<String>  skippedSkus = new ArrayList<>();
        List<String>  errors      = new ArrayList<>();
        List<Product> batch       = new ArrayList<>(BATCH_SIZE);
        List<BulkUploadRow> currentBatchRows = new ArrayList<>(BATCH_SIZE);
        List<BulkUploadRow> uploadRows = new ArrayList<>();
        Set<String> supplierNamesInFile = new LinkedHashSet<>();
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

        // CSV files use a completely different parse path
        if (isCsvFile(file)) {
            return importFromCsv(file, upload, skippedSkus, errors, batch, currentBatchRows,
                    uploadRows, supplierNamesInFile, categoryCache, supplierCache,
                    existingSkus, existingBarcodes, barcodeCountInFile,
                    autoCreateSuppliers);
        }

        // .xlsx and .xls — WorkbookFactory auto-detects both formats
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {

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

                BulkUploadRow uploadRow = createUploadRowSnapshot(upload, row, excelRowNum);
                uploadRows.add(uploadRow);
                if (uploadRow.getSupplierName() != null && !uploadRow.getSupplierName().isBlank()) {
                    supplierNamesInFile.add(uploadRow.getSupplierName());
                }

                try {
                    String rawBarcode = uploadRow.getSkuBarcode() != null ? uploadRow.getSkuBarcode() : "";

                    if (rawBarcode.isBlank()) {
                        String message = "SKU / Barcode (Column B) is required";
                        errors.add("Row " + excelRowNum + ": " + message);
                        markUploadRow(uploadRow, "FAILED", message, null);
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

                    // ── ADD MODE: Update existing product or create new ────────────────
                    Product product;
                    if (existingSkus.contains(sku)) {
                        // Fetch existing product
                        product = productRepository.findBySku(sku).orElseThrow(() -> new IllegalStateException("SKU exists but not found"));
                        
                        // Update details
                        updateProductFromRow(product, row, categoryCache, supplierCache, autoCreateSuppliers, excelRowNum);
                        
                        // Add opening stock to current stock (if any)
                        BigDecimal incomingStock = parseBigDecimalOptional(row, COL_OPENING_STOCK);
                        if (incomingStock != null && incomingStock.compareTo(BigDecimal.ZERO) > 0) {
                            product.setCurrentStock(product.getCurrentStock().add(incomingStock));
                            // We need to pass the *incoming* stock amount to markUploadRow so we can record it in the ledger later
                            uploadRow.setOpeningStock(incomingStock);
                        } else {
                            uploadRow.setOpeningStock(BigDecimal.ZERO);
                        }
                    } else {
                        product = mapRowToProduct(row, excelRowNum, sku, rawBarcode, categoryCache, supplierCache, autoCreateSuppliers);
                        // Save incoming stock in row for ledger
                        uploadRow.setOpeningStock(product.getCurrentStock() != null ? product.getCurrentStock() : BigDecimal.ZERO);
                    }

                    batch.add(product);
                    currentBatchRows.add(uploadRow);
                    markUploadRow(uploadRow, "SUCCESS", null, product);
                    existingSkus.add(sku);
                    existingBarcodes.add(rawBarcode);

                    // Flush batch to DB when full
                    if (batch.size() == BATCH_SIZE) {
                        productRepository.saveAll(batch);
                        recordStockLedgerForBatch(currentBatchRows);
                        successCount += batch.size();
                        log.info("Batch saved — {} products inserted so far", successCount);
                        batch.clear();
                        currentBatchRows.clear();
                    }

                } catch (Exception e) {
                    errors.add("Row " + excelRowNum + ": " + e.getMessage());
                    markUploadRow(uploadRow, "FAILED", e.getMessage(), null);
                    log.warn("Row {} failed: {}", excelRowNum, e.getMessage());
                }
            }

            // Save the final partial batch (remainder < 500)
            if (!batch.isEmpty()) {
                productRepository.saveAll(batch);
                recordStockLedgerForBatch(currentBatchRows);
                successCount += batch.size();
            }

        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read XLSX file: " + e.getMessage());
        }

        log.info("Bulk import complete — success={}, skipped={}, duplicateBarcodes={}, failed={}",
                successCount, skippedCount, duplicateBarcodeCount, errors.size());

        upload.setTotalRows(totalRows);
        upload.setSuccessCount(successCount);
        upload.setSkippedCount(skippedCount);
        upload.setFailedCount(errors.size());
        upload.setDuplicateBarcodeCount(duplicateBarcodeCount);
        upload.setStatus(resolveUploadStatus(successCount, skippedCount, errors.size()));
        bulkUploadRepository.save(upload);
        bulkUploadRowRepository.saveAll(uploadRows);
        saveTemplatesForSuppliers(supplierNamesInFile, supplierCache, upload);

        return BulkImportResponseDTO.builder()
                .bulkUploadId(upload.getId())
                .status(upload.getStatus())
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

    private String calculateFileHash(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            
            // Hash the filename first
            String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown";
            digest.update(filename.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            
            // Then hash the file contents
            byte[] hashBytes = digest.digest(file.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to calculate file hash", e);
        }
    }

    private void recordStockLedgerForBatch(List<BulkUploadRow> batchRows) {
        List<StockLedger> ledgers = new ArrayList<>();
        for (BulkUploadRow row : batchRows) {
            Product product = row.getProduct();
            BigDecimal incomingStock = row.getOpeningStock();
            if (product != null && incomingStock != null && incomingStock.compareTo(BigDecimal.ZERO) > 0) {
                ledgers.add(StockLedger.builder()
                        .product(product)
                        .transactionType("OPENING_STOCK")
                        .quantityIn(incomingStock)
                        .balanceStock(product.getCurrentStock())
                        .transactionDate(java.time.LocalDateTime.now())
                        .reason("Bulk Import Initial Stock")
                        .build());
            }
        }
        if (!ledgers.isEmpty()) {
            stockLedgerRepository.saveAll(ledgers);
        }
    }

    private BulkUploadRow createUploadRowSnapshot(BulkUpload upload, Row row, int excelRowNum) {
        return BulkUploadRow.builder()
                .upload(upload)
                .rowNumber(excelRowNum)
                .status("PENDING")
                .productName(getCellString(row, COL_NAME))
                .skuBarcode(getCellString(row, COL_SKU))
                .category(getCellString(row, COL_CATEGORY))
                .unitOfMeasure(getCellString(row, COL_UNIT))
                .purchaseRate(parseBigDecimalSnapshot(row, COL_PURCHASE_RATE))
                .mrp(parseBigDecimalSnapshot(row, COL_MRP))
                .gstRate(parseBigDecimalSnapshot(row, COL_GST_RATE))
                .hsnCode(getCellString(row, COL_HSN_CODE))
                .openingStock(parseBigDecimalSnapshot(row, COL_OPENING_STOCK))
                .minStock(parseBigDecimalSnapshot(row, COL_MIN_STOCK))
                .description(getCellString(row, COL_DESCRIPTION))
                .brand(getCellString(row, COL_BRAND))
                .supplierName(getCellString(row, COL_SUPPLIER_NAME))
                .expiry(getCellString(row, COL_EXPIRY))
                .active(getCellString(row, COL_ACTIVE))
                .build();
    }

    private void markUploadRow(BulkUploadRow row, String status, String errorMessage, Product product) {
        row.setStatus(status);
        row.setErrorMessage(errorMessage);
        row.setProduct(product);
    }

    private String resolveUploadStatus(int successCount, int skippedCount, int failedCount) {
        if (successCount == 0 && failedCount > 0 && skippedCount == 0) {
            return "FAILED";
        }
        if (failedCount > 0 || skippedCount > 0) {
            return "PARTIAL";
        }
        return "SUCCESS";
    }

    private void saveTemplatesForSuppliers(Set<String> supplierNames,
                                           Map<String, Supplier> supplierCache,
                                           BulkUpload sourceUpload) {
        String headersJson = writeHeadersJson();
        for (String supplierName : supplierNames) {
            String normalized = normalizeSupplierName(supplierName);
            if (normalized.isBlank()) {
                continue;
            }
            BulkUploadTemplate template = bulkUploadTemplateRepository.findByNormalizedSupplierName(normalized)
                    .orElseGet(() -> BulkUploadTemplate.builder()
                            .supplierNameSnapshot(supplierName)
                            .normalizedSupplierName(normalized)
                            .sourceUpload(sourceUpload)
                            .build());

            template.setSupplier(supplierCache.get(normalized));
            template.setSupplierNameSnapshot(supplierName);
            template.setTemplateName("Products Master");
            template.setHeadersJson(headersJson);
            template.setColumnCount(TEMPLATE_HEADERS.length);
            template.setLastUsedAt(java.time.LocalDateTime.now());
            bulkUploadTemplateRepository.save(template);
        }
    }

    private String normalizeSupplierName(String supplierName) {
        return supplierName == null ? "" : supplierName.trim().toLowerCase();
    }

    private String writeHeadersJson() {
        try {
            return objectMapper.writeValueAsString(Arrays.asList(TEMPLATE_HEADERS));
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize bulk upload template headers");
        }
    }

    private BigDecimal parseBigDecimalSnapshot(Row row, int colIdx) {
        String val = getCellString(row, colIdx);
        if (val.isBlank()) return null;
        try {
            return new BigDecimal(val);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Accepted formats:
     *   .xlsx — Excel 2007+ (recommended, BillPro template)
     *   .xls  — Excel 97-2003 (legacy, auto-detected via WorkbookFactory)
     *   .csv  — Comma-separated values (header row on row 1, data from row 2)
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Upload file is empty");
        }
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("Filename is missing. Please re-upload the file.");
        }
        String lower = filename.toLowerCase();
        if (!lower.endsWith(".xlsx") && !lower.endsWith(".xls") && !lower.endsWith(".csv")) {
            throw new IllegalArgumentException(
                    "Unsupported file format: '" + filename + "'. "
                    + "Accepted formats: .xlsx (recommended), .xls, .csv");
        }
    }

    /** Returns true if the uploaded file is a CSV (plain text, not Excel) */
    private boolean isCsvFile(MultipartFile file) {
        String name = file.getOriginalFilename();
        return name != null && name.toLowerCase().endsWith(".csv");
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

    /** Updates an existing product's details using Add Mode logic */
    private void updateProductFromRow(Product product, Row row,
                                      Map<String, Category> categoryCache,
                                      Map<String, Supplier> supplierCache,
                                      boolean autoCreateSuppliers, int excelRowNum) {
        String name = getCellString(row, COL_NAME);
        if (!name.isBlank()) product.setName(name);

        String categoryName = getCellString(row, COL_CATEGORY);
        if (!categoryName.isBlank()) product.setCategory(categoryCache.computeIfAbsent(categoryName, this::findOrCreateCategory));

        String unit = getCellString(row, COL_UNIT);
        if (!unit.isBlank()) product.setUnit(unit);

        BigDecimal purchaseRate = parseBigDecimalRequired(row, COL_PURCHASE_RATE, "Purchase Rate (Column E)", excelRowNum);
        BigDecimal mrp          = parseBigDecimalRequired(row, COL_MRP, "MRP/Selling Price (Column F)", excelRowNum);
        
        if (mrp.compareTo(purchaseRate) < 0) {
            throw new IllegalArgumentException("MRP (" + mrp + ") must be ≥ Purchase Rate (" + purchaseRate + ")");
        }
        product.setPurchaseRate(purchaseRate);
        product.setMrp(mrp);
        product.setSellingPrice(mrp);

        BigDecimal gstRate = parseBigDecimalRequired(row, COL_GST_RATE, "GST % (Column G)", excelRowNum);
        BigDecimal gstRounded = gstRate.stripTrailingZeros().setScale(0, java.math.RoundingMode.UNNECESSARY);
        if (!VALID_GST_RATES.contains(gstRate) && !VALID_GST_RATES.contains(gstRounded)) {
            throw new IllegalArgumentException("Invalid GST rate: " + gstRate + ". Must be one of: 0, 5, 12, 18, 28");
        }
        product.setGstRate(gstRate);

        String hsnCode = getCellString(row, COL_HSN_CODE);
        if (!hsnCode.isBlank()) product.setHsnCode(hsnCode);

        // Supplier update logic
        String supplierName = getCellString(row, COL_SUPPLIER_NAME);
        if (!supplierName.isBlank()) {
            Supplier primarySupplier = supplierCache.get(supplierName.toLowerCase());
            if (primarySupplier == null) {
                if (autoCreateSuppliers) {
                    primarySupplier = supplierRepository.save(Supplier.builder().name(supplierName).build());
                    supplierCache.put(supplierName.toLowerCase(), primarySupplier);
                } else {
                    throw new IllegalArgumentException("Supplier '" + supplierName + "' not found in system.");
                }
            }
            product.setPrimarySupplier(primarySupplier);
        }

        BigDecimal minStock = parseBigDecimalOptional(row, COL_MIN_STOCK);
        if (minStock != null) product.setMinStock(minStock);

        String description = getCellString(row, COL_DESCRIPTION);
        String brand = getCellString(row, COL_BRAND);
        String expiry = getCellString(row, COL_EXPIRY);

        if (!expiry.isBlank()) {
            description = description.isBlank() ? "Shelf Life: " + expiry : description + " | Shelf Life: " + expiry;
        }
        if (!description.isBlank()) product.setDescription(description);
        if (!brand.isBlank()) product.setBrand(brand);

        String activeCell = getCellString(row, COL_ACTIVE).toUpperCase().trim();
        if (!activeCell.isBlank()) {
            product.setIsActive(!activeCell.equals("NO"));
        }
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

    // ─── CSV Import ─────────────────────────────────────────────────────────────

    /**
     * Imports products from a CSV file.
     *
     * CSV format expected:
     *   Row 1: Header row  (same column names as the XLSX template — skipped automatically)
     *   Row 2+: Data rows
     *
     * Column order MUST match the XLSX template (columns A–O):
     *   Product Name, SKU/Barcode, Category, UoM, Purchase Rate, MRP,
     *   GST%, HSN Code, Opening Stock, Min Stock, Description, Brand,
     *   Supplier Name, Expiry, Active
     *
     * The method shares ALL business logic with the Excel path via String[] overloads.
     */
    @SuppressWarnings("java:S107")
    private BulkImportResponseDTO importFromCsv(
            MultipartFile file,
            BulkUpload upload,
            List<String>  skippedSkus,
            List<String>  errors,
            List<Product> batch,
            List<BulkUploadRow> currentBatchRows,
            List<BulkUploadRow> uploadRows,
            Set<String>         supplierNamesInFile,
            Map<String, Category> categoryCache,
            Map<String, Supplier> supplierCache,
            Set<String> existingSkus,
            Set<String> existingBarcodes,
            Map<String, Integer> barcodeCountInFile,
            boolean autoCreateSuppliers) {

        int successCount = 0, skippedCount = 0, totalRows = 0, duplicateBarcodeCount = 0;

        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                     new java.io.InputStreamReader(file.getInputStream(),
                             java.nio.charset.StandardCharsets.UTF_8));
             CSVParser csvParser = CSVFormat.DEFAULT
                     .builder()
                     .setHeader()           // first row treated as header — auto-skipped
                     .setSkipHeaderRecord(true)
                     .setTrim(true)
                     .setIgnoreEmptyLines(true)
                     .setIgnoreSurroundingSpaces(true)
                     .build()
                     .parse(reader)) {

            List<CSVRecord> records = csvParser.getRecords();
            totalRows = records.size();

            if (totalRows > MAX_ROWS) {
                throw new IllegalArgumentException(
                        "CSV has " + totalRows + " rows. BillPro max is "
                        + MAX_ROWS + " rows per upload. Split into multiple files.");
            }

            log.info("CSV bulk import started — {} data rows", totalRows);

            for (CSVRecord record : records) {
                int csvRowNum = (int) record.getRecordNumber() + 1; // +1 for header row offset

                // Convert CSVRecord to String[] (same length as TEMPLATE_HEADERS)
                String[] cells = csvRecordToCells(record);

                // Skip completely empty rows
                if (isCellArrayEmpty(cells)) {
                    totalRows--;
                    continue;
                }

                BulkUploadRow uploadRow = createUploadRowSnapshotFromCells(upload, cells, csvRowNum);
                uploadRows.add(uploadRow);
                if (uploadRow.getSupplierName() != null && !uploadRow.getSupplierName().isBlank()) {
                    supplierNamesInFile.add(uploadRow.getSupplierName());
                }

                try {
                    String rawBarcode = uploadRow.getSkuBarcode() != null ? uploadRow.getSkuBarcode() : "";

                    if (rawBarcode.isBlank()) {
                        String msg = "SKU / Barcode (Column B) is required";
                        errors.add("Row " + csvRowNum + ": " + msg);
                        markUploadRow(uploadRow, "FAILED", msg, null);
                        continue;
                    }

                    int occurrence = barcodeCountInFile.merge(rawBarcode, 1, Integer::sum);
                    String sku = (occurrence == 1) ? rawBarcode : rawBarcode + "-DUP-" + occurrence;

                    if (occurrence > 1 || existingBarcodes.contains(rawBarcode)) {
                        duplicateBarcodeCount++;
                    }

                    Product product;
                    if (existingSkus.contains(sku)) {
                        product = productRepository.findBySku(sku)
                                .orElseThrow(() -> new IllegalStateException("SKU exists but not found"));
                        updateProductFromCells(product, cells, categoryCache, supplierCache,
                                autoCreateSuppliers, csvRowNum);
                        BigDecimal incomingStock = parseBigDecimalOptionalFromCell(cells[COL_OPENING_STOCK]);
                        if (incomingStock != null && incomingStock.compareTo(BigDecimal.ZERO) > 0) {
                            product.setCurrentStock(product.getCurrentStock().add(incomingStock));
                            uploadRow.setOpeningStock(incomingStock);
                        } else {
                            uploadRow.setOpeningStock(BigDecimal.ZERO);
                        }
                    } else {
                        product = mapCellsToProduct(cells, csvRowNum, sku, rawBarcode,
                                categoryCache, supplierCache, autoCreateSuppliers);
                        uploadRow.setOpeningStock(product.getCurrentStock() != null
                                ? product.getCurrentStock() : BigDecimal.ZERO);
                    }

                    batch.add(product);
                    currentBatchRows.add(uploadRow);
                    markUploadRow(uploadRow, "SUCCESS", null, product);
                    existingSkus.add(sku);
                    existingBarcodes.add(rawBarcode);

                    if (batch.size() == BATCH_SIZE) {
                        productRepository.saveAll(batch);
                        recordStockLedgerForBatch(currentBatchRows);
                        successCount += batch.size();
                        log.info("CSV batch saved — {} products so far", successCount);
                        batch.clear();
                        currentBatchRows.clear();
                    }

                } catch (Exception e) {
                    errors.add("Row " + csvRowNum + ": " + e.getMessage());
                    markUploadRow(uploadRow, "FAILED", e.getMessage(), null);
                    log.warn("CSV row {} failed: {}", csvRowNum, e.getMessage());
                }
            }

            if (!batch.isEmpty()) {
                productRepository.saveAll(batch);
                recordStockLedgerForBatch(currentBatchRows);
                successCount += batch.size();
            }

        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read CSV file: " + e.getMessage());
        }

        upload.setTotalRows(totalRows);
        upload.setSuccessCount(successCount);
        upload.setSkippedCount(skippedCount);
        upload.setFailedCount(errors.size());
        upload.setDuplicateBarcodeCount(duplicateBarcodeCount);
        upload.setStatus(resolveUploadStatus(successCount, skippedCount, errors.size()));
        bulkUploadRepository.save(upload);
        bulkUploadRowRepository.saveAll(uploadRows);

        log.info("CSV import complete — success={}, failed={}", successCount, errors.size());

        return BulkImportResponseDTO.builder()
                .bulkUploadId(upload.getId())
                .status(upload.getStatus())
                .totalRows(totalRows)
                .successCount(successCount)
                .skippedCount(skippedCount)
                .failedCount(errors.size())
                .duplicateBarcodeCount(duplicateBarcodeCount)
                .skippedSkus(skippedSkus)
                .errors(errors)
                .build();
    }

    // ─── CSV Helper Methods ──────────────────────────────────────────────────────

    /**
     * Converts a CSVRecord to a String[] with exactly TEMPLATE_HEADERS.length slots.
     * Missing columns are filled with "". Handles files with fewer columns gracefully.
     */
    private String[] csvRecordToCells(CSVRecord record) {
        String[] cells = new String[TEMPLATE_HEADERS.length];
        Arrays.fill(cells, "");
        for (int i = 0; i < TEMPLATE_HEADERS.length && i < record.size(); i++) {
            String val = record.get(i);
            cells[i] = val != null ? val.trim() : "";
        }
        return cells;
    }

    private boolean isCellArrayEmpty(String[] cells) {
        for (String cell : cells) {
            if (cell != null && !cell.isBlank()) return false;
        }
        return true;
    }

    private BulkUploadRow createUploadRowSnapshotFromCells(BulkUpload upload, String[] c, int rowNum) {
        return BulkUploadRow.builder()
                .upload(upload)
                .rowNumber(rowNum)
                .status("PENDING")
                .productName(c[COL_NAME])
                .skuBarcode(c[COL_SKU])
                .category(c[COL_CATEGORY])
                .unitOfMeasure(c[COL_UNIT])
                .purchaseRate(parseBigDecimalOptionalFromCell(c[COL_PURCHASE_RATE]))
                .mrp(parseBigDecimalOptionalFromCell(c[COL_MRP]))
                .gstRate(parseBigDecimalOptionalFromCell(c[COL_GST_RATE]))
                .hsnCode(c[COL_HSN_CODE])
                .openingStock(parseBigDecimalOptionalFromCell(c[COL_OPENING_STOCK]))
                .minStock(parseBigDecimalOptionalFromCell(c[COL_MIN_STOCK]))
                .description(c[COL_DESCRIPTION])
                .brand(c[COL_BRAND])
                .supplierName(c[COL_SUPPLIER_NAME])
                .expiry(c[COL_EXPIRY])
                .active(c[COL_ACTIVE])
                .build();
    }

    /** Maps a String[] (from CSV) to a new Product entity — mirrors mapRowToProduct exactly. */
    private Product mapCellsToProduct(String[] c, int rowNum, String sku, String rawBarcode,
                                       Map<String, Category> categoryCache,
                                       Map<String, Supplier> supplierCache,
                                       boolean autoCreateSuppliers) {
        String name = c[COL_NAME];
        if (name.isBlank()) throw new IllegalArgumentException("Product Name (Column A) is required");

        String categoryName = c[COL_CATEGORY];
        if (categoryName.isBlank()) throw new IllegalArgumentException("Category (Column C) is required");
        Category category = categoryCache.computeIfAbsent(categoryName, this::findOrCreateCategory);

        String unit = c[COL_UNIT];
        if (unit.isBlank()) throw new IllegalArgumentException("Unit of Measure (Column D) is required");

        BigDecimal purchaseRate = parseBigDecimalRequiredFromCell(c[COL_PURCHASE_RATE], "Purchase Rate (Column E)");
        BigDecimal mrp          = parseBigDecimalRequiredFromCell(c[COL_MRP], "MRP/Selling Price (Column F)");

        if (mrp.compareTo(purchaseRate) < 0) {
            throw new IllegalArgumentException(
                    "MRP (" + mrp + ") must be \u2265 Purchase Rate (" + purchaseRate + ")");
        }

        BigDecimal gstRate    = parseBigDecimalRequiredFromCell(c[COL_GST_RATE], "GST % (Column G)");
        BigDecimal gstRounded = gstRate.stripTrailingZeros().setScale(0, java.math.RoundingMode.UNNECESSARY);
        if (!VALID_GST_RATES.contains(gstRate) && !VALID_GST_RATES.contains(gstRounded)) {
            throw new IllegalArgumentException(
                    "Invalid GST rate: " + gstRate + ". Must be one of: 0, 5, 12, 18, 28");
        }

        String hsnCode = c[COL_HSN_CODE];
        if (hsnCode.isBlank()) throw new IllegalArgumentException("HSN Code (Column H) is required");

        String supplierName = c[COL_SUPPLIER_NAME];
        if (supplierName.isBlank()) {
            throw new IllegalArgumentException(
                    "Supplier Name (Column M) is required for traceability.");
        }

        Supplier primarySupplier = supplierCache.get(supplierName.toLowerCase());
        if (primarySupplier == null) {
            if (autoCreateSuppliers) {
                primarySupplier = supplierRepository.save(Supplier.builder().name(supplierName).build());
                supplierCache.put(supplierName.toLowerCase(), primarySupplier);
            } else {
                throw new IllegalArgumentException(
                        "Supplier '" + supplierName + "' (Column M) not found. "
                        + "Add it in Suppliers module first.");
            }
        }

        BigDecimal openingStock = parseBigDecimalOptionalFromCell(c[COL_OPENING_STOCK]);
        BigDecimal minStock     = parseBigDecimalOptionalFromCell(c[COL_MIN_STOCK]);
        String description      = c[COL_DESCRIPTION];
        String expiry           = c[COL_EXPIRY];
        if (!expiry.isBlank()) {
            description = description.isBlank() ? "Expiry: " + expiry : description + " | Expiry: " + expiry;
        }
        String activeStr = c[COL_ACTIVE];
        boolean isActive = activeStr.isBlank() || activeStr.equalsIgnoreCase("YES") || activeStr.equalsIgnoreCase("Y");

        return Product.builder()
                .name(name)
                .sku(sku)
                .barcode(rawBarcode)
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
                .brand(c[COL_BRAND].isBlank() ? null : c[COL_BRAND])
                .isActive(isActive)
                .build();
    }

    /** Updates an existing Product from a CSV String[] — mirrors updateProductFromRow. */
    private void updateProductFromCells(Product product, String[] c,
                                         Map<String, Category> categoryCache,
                                         Map<String, Supplier> supplierCache,
                                         boolean autoCreateSuppliers, int rowNum) {
        String name = c[COL_NAME];
        if (!name.isBlank()) product.setName(name);

        String categoryName = c[COL_CATEGORY];
        if (!categoryName.isBlank()) {
            product.setCategory(categoryCache.computeIfAbsent(categoryName, this::findOrCreateCategory));
        }

        String unit = c[COL_UNIT];
        if (!unit.isBlank()) product.setUnit(unit);

        BigDecimal mrp = parseBigDecimalOptionalFromCell(c[COL_MRP]);
        if (mrp != null) {
            product.setMrp(mrp);
            product.setSellingPrice(mrp);
        }

        BigDecimal purchaseRate = parseBigDecimalOptionalFromCell(c[COL_PURCHASE_RATE]);
        if (purchaseRate != null) product.setPurchaseRate(purchaseRate);

        BigDecimal gstRate = parseBigDecimalOptionalFromCell(c[COL_GST_RATE]);
        if (gstRate != null) product.setGstRate(gstRate);

        String hsnCode = c[COL_HSN_CODE];
        if (!hsnCode.isBlank()) product.setHsnCode(hsnCode);

        BigDecimal minStock = parseBigDecimalOptionalFromCell(c[COL_MIN_STOCK]);
        if (minStock != null) product.setMinStock(minStock);

        String description = c[COL_DESCRIPTION];
        String expiry = c[COL_EXPIRY];
        if (!expiry.isBlank()) {
            description = description.isBlank() ? "Expiry: " + expiry : description + " | Expiry: " + expiry;
        }
        if (!description.isBlank()) product.setDescription(description);

        String brand = c[COL_BRAND];
        if (!brand.isBlank()) product.setBrand(brand);

        String supplierName = c[COL_SUPPLIER_NAME];
        if (!supplierName.isBlank()) {
            Supplier supplier = supplierCache.get(supplierName.toLowerCase());
            if (supplier == null && autoCreateSuppliers) {
                supplier = supplierRepository.save(Supplier.builder().name(supplierName).build());
                supplierCache.put(supplierName.toLowerCase(), supplier);
            }
            if (supplier != null) product.setPrimarySupplier(supplier);
        }
    }

    /** Parse required BigDecimal from a CSV cell string */
    private BigDecimal parseBigDecimalRequiredFromCell(String val, String fieldName) {
        if (val == null || val.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required but empty");
        }
        try {
            return new BigDecimal(val.replace(",", "").replace("\u20b9", "").trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    fieldName + " has invalid value '" + val
                    + "'. Use plain numbers only (e.g. 250.00)");
        }
    }

    /** Parse optional BigDecimal from a CSV cell string — returns null if blank */
    private BigDecimal parseBigDecimalOptionalFromCell(String val) {
        if (val == null || val.isBlank()) return null;
        try {
            return new BigDecimal(val.replace(",", "").replace("\u20b9", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
