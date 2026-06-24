package com.Billing_System.vendor.service;

import com.Billing_System.vendor.dto.VendorBulkImportResponseDTO;
import com.Billing_System.vendor.entity.Vendor;
import com.Billing_System.vendor.entity.VendorProduct;
import com.Billing_System.vendor.repository.VendorProductRepository;
import com.Billing_System.vendor.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * VendorBulkImportService — Imports vendor product catalogs from XLSX.
 *
 * ═══ Excel Format (Vendor Products Sheet) ═══
 * Row 1: Legend banner (colour-coded) — DO NOT EDIT
 * Row 2: Column headers (dark blue)   — DO NOT EDIT
 * Row 3: Sample data (blue italic)    — DELETE before upload
 * Row 4+: Actual data
 *
 * Column mapping (0-indexed):
 *  0  - GSTIN             (REQUIRED — vendor's GSTIN; auto-created during vendor master upload)
 *  1  - Product Name     (REQUIRED)
 *  2  - Vendor SKU       (REQUIRED — vendor's own product code, unique per vendor)
 *  3  - Purchase Price ₹ (REQUIRED)
 *  4  - Unit of Measure  (REQUIRED — KG/PCS/LTR/BOX)
 *  5  - Pack Size        (optional — 500ml / 1kg / 12pcs)
 *  6  - GST %            (REQUIRED — 0/5/12/18/28)
 *  7  - HSN Code         (optional)
 *  8  - Brand            (optional)
 *  9  - Category         (optional)
 * 10  - Min Order Qty    (optional)
 * 11  - Description      (optional)
 *
 * VENDOR IDENTIFICATION:
 *   Use GSTIN in column 0 (not vendor code).
 *   Vendor must exist in vendors table (upload vendor master first).
 *
 * Max 5,000 rows per file.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VendorBulkImportService {

    private static final int BATCH_SIZE    = 500;
    private static final int MAX_ROWS      = 5000;
    private static final int DATA_START_ROW = 3; // 0-indexed: Row 4 in Excel = index 3

    // Column indices (0-based)
    private static final int COL_GSTIN          = 0;  // changed from COL_VENDOR_CODE
    private static final int COL_PRODUCT_NAME   = 1;
    private static final int COL_VENDOR_SKU     = 2;
    private static final int COL_PURCHASE_PRICE = 3;
    private static final int COL_UNIT           = 4;
    private static final int COL_PACK_SIZE      = 5;
    private static final int COL_GST_RATE       = 6;
    private static final int COL_HSN_CODE       = 7;
    private static final int COL_BRAND          = 8;
    private static final int COL_CATEGORY       = 9;
    private static final int COL_MIN_ORDER_QTY  = 10;
    private static final int COL_DESCRIPTION    = 11;

    public static final String[] TEMPLATE_HEADERS = {
            "GSTIN",
            "Product Name",
            "Vendor SKU",
            "Purchase Price (₹)",
            "Unit of Measure",
            "Pack Size",
            "GST %",
            "HSN Code",
            "Brand",
            "Category",
            "Min Order Qty",
            "Description"
    };

    private static final Set<BigDecimal> VALID_GST_RATES = Set.of(
            BigDecimal.ZERO,
            new BigDecimal("5"),
            new BigDecimal("12"),
            new BigDecimal("18"),
            new BigDecimal("28")
    );

    private final VendorRepository        vendorRepository;
    private final VendorProductRepository vendorProductRepository;
    private final com.Billing_System.vendor.repository.VendorCategoryRepository vendorCategoryRepository;

    /**
     * Main entry point — parse XLSX, upsert vendor products, return import report.
     *
     * @param file the uploaded .xlsx file (max 5000 rows)
     */
    @Transactional
    public VendorBulkImportResponseDTO importFromXlsx(MultipartFile file) {
        validateFile(file);

        // Pre-load ALL vendors into memory: GSTIN (uppercase) → Vendor
        Map<String, Vendor> vendorCache = new HashMap<>();
        vendorRepository.findAll().forEach(v -> {
            if (v.getGstin() != null)
                vendorCache.put(v.getGstin().toUpperCase().replace(" ", ""), v);
        });

        List<String> errors              = new ArrayList<>();
        List<String> skippedSkus         = new ArrayList<>();
        List<String> vendorCodesNotFound = new ArrayList<>();
        List<VendorProduct> batch        = new ArrayList<>(BATCH_SIZE);

        int successCount = 0;
        int failedCount  = 0;
        int skippedCount = 0;
        int updatedCount = 0;
        int totalRows    = 0;

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet      = workbook.getSheetAt(0);
            int   lastRowNum = sheet.getLastRowNum();

            if (lastRowNum < DATA_START_ROW) {
                throw new IllegalArgumentException("Excel file has no data rows. Add product data from row 4 onwards.");
            }

            int dataRows = lastRowNum - DATA_START_ROW + 1;
            if (dataRows > MAX_ROWS) {
                throw new IllegalArgumentException(
                    "File has " + dataRows + " rows. Maximum allowed is " + MAX_ROWS + " rows per upload.");
            }

            for (int rowIdx = DATA_START_ROW; rowIdx <= lastRowNum; rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null || isRowEmpty(row)) continue;

                totalRows++;
                int excelRow = rowIdx + 1; // 1-indexed for error messages

                try {
                    // ── Read required fields ──────────────────────────────────
                    String gstin        = getCellString(row, COL_GSTIN).toUpperCase().replace(" ", "");
                    String productName  = getCellString(row, COL_PRODUCT_NAME);
                    String vendorSku    = getCellString(row, COL_VENDOR_SKU);
                    String priceStr     = getCellString(row, COL_PURCHASE_PRICE);
                    String unit         = getCellString(row, COL_UNIT);
                    String gstStr       = getCellString(row, COL_GST_RATE);

                    // ── Validate required fields ──────────────────────────────
                    List<String> missing = new ArrayList<>();
                    if (gstin.isBlank())       missing.add("GSTIN");
                    if (productName.isBlank()) missing.add("Product Name");
                    if (vendorSku.isBlank())   missing.add("Vendor SKU");
                    if (priceStr.isBlank())    missing.add("Purchase Price");
                    if (unit.isBlank())        missing.add("Unit of Measure");
                    if (gstStr.isBlank())      missing.add("GST %");

                    if (!missing.isEmpty()) {
                        errors.add("Row " + excelRow + ": Missing required fields: " + String.join(", ", missing));
                        failedCount++;
                        continue;
                    }

                    // ── Vendor lookup ─────────────────────────────────────────
                    Vendor vendor = vendorCache.get(gstin);
                    if (vendor == null) {
                        if (!vendorCodesNotFound.contains(gstin)) {
                            vendorCodesNotFound.add(gstin);
                        }
                        errors.add("Row " + excelRow + ": GSTIN '" + gstin + "' not found. Create the vendor first.");
                        failedCount++;
                        continue;
                    }

                    // ── Vendor must be ACTIVE ─────────────────────────────────
                    if (!"ACTIVE".equals(vendor.getKycStatus())) {
                        errors.add("Row " + excelRow + ": Vendor GSTIN '" + gstin + "' is not ACTIVE (status: "
                                + vendor.getKycStatus() + "). Complete onboarding first.");
                        failedCount++;
                        continue;
                    }

                    // ── Parse purchase price ──────────────────────────────────
                    BigDecimal purchasePrice;
                    try {
                        purchasePrice = new BigDecimal(priceStr.replace(",", "").trim());
                        if (purchasePrice.compareTo(BigDecimal.ZERO) < 0) {
                            throw new NumberFormatException("negative");
                        }
                    } catch (NumberFormatException e) {
                        errors.add("Row " + excelRow + ": Invalid Purchase Price '" + priceStr + "'. Must be a positive number.");
                        failedCount++;
                        continue;
                    }

                    // ── Parse GST rate ────────────────────────────────────────
                    BigDecimal gstRate;
                    try {
                        gstRate = new BigDecimal(gstStr.replace("%", "").trim()).stripTrailingZeros();
                        if (!VALID_GST_RATES.contains(gstRate)) {
                            errors.add("Row " + excelRow + ": Invalid GST rate '" + gstStr + "'. Must be 0, 5, 12, 18, or 28.");
                            failedCount++;
                            continue;
                        }
                    } catch (NumberFormatException e) {
                        errors.add("Row " + excelRow + ": Invalid GST format '" + gstStr + "'.");
                        failedCount++;
                        continue;
                    }

                    // ── Optional fields ───────────────────────────────────────
                    String packSize    = getCellString(row, COL_PACK_SIZE);
                    String hsnCode     = getCellString(row, COL_HSN_CODE);
                    String brand       = getCellString(row, COL_BRAND);
                    String category    = getCellString(row, COL_CATEGORY);
                    String description = getCellString(row, COL_DESCRIPTION);
                    String minQtyStr   = getCellString(row, COL_MIN_ORDER_QTY);

                    BigDecimal minOrderQty = null;
                    if (!minQtyStr.isBlank()) {
                        try {
                            minOrderQty = new BigDecimal(minQtyStr.replace(",", "").trim());
                        } catch (NumberFormatException e) {
                            // non-critical — just ignore invalid min qty
                            log.warn("Row {}: Invalid Min Order Qty '{}' — ignored", excelRow, minQtyStr);
                        }
                    }

                    // --- Upsert Category if it exists ---
                    if (category != null && !category.isBlank()) {
                        String catTrimmed = category.trim();
                        // Find or create category
                        if (vendorCategoryRepository.findByNameIgnoreCase(catTrimmed).isEmpty()) {
                            com.Billing_System.vendor.entity.VendorCategory newCat = 
                                com.Billing_System.vendor.entity.VendorCategory.builder()
                                    .name(catTrimmed)
                                    .color("slate")
                                    .build();
                            vendorCategoryRepository.save(newCat);
                        }
                    }

                    // ── Upsert: update if exists, insert if new ───────────────
                    Optional<VendorProduct> existing =
                            vendorProductRepository.findByVendorIdAndVendorSku(vendor.getId(), vendorSku.trim());

                    VendorProduct product;
                    if (existing.isPresent()) {
                        // UPDATE existing
                        product = existing.get();
                        product.setProductName(productName.trim());
                        product.setPurchasePrice(purchasePrice);
                        product.setUnitOfMeasure(unit.trim().toUpperCase());
                        product.setPackSize(packSize.isBlank() ? null : packSize.trim());
                        product.setGstRate(gstRate);
                        product.setHsnCode(hsnCode.isBlank() ? null : hsnCode.trim());
                        product.setBrand(brand.isBlank() ? null : brand.trim());
                        product.setCategory(category.isBlank() ? null : category.trim());
                        product.setMinOrderQty(minOrderQty);
                        product.setDescription(description.isBlank() ? null : description.trim());
                        product.setUpdatedAt(LocalDateTime.now());
                        product.setActive(true);
                        updatedCount++;
                    } else {
                        // INSERT new
                        product = VendorProduct.builder()
                                .vendor(vendor)
                                .productName(productName.trim())
                                .vendorSku(vendorSku.trim())
                                .purchasePrice(purchasePrice)
                                .unitOfMeasure(unit.trim().toUpperCase())
                                .packSize(packSize.isBlank() ? null : packSize.trim())
                                .gstRate(gstRate)
                                .hsnCode(hsnCode.isBlank() ? null : hsnCode.trim())
                                .brand(brand.isBlank() ? null : brand.trim())
                                .category(category.isBlank() ? null : category.trim())
                                .minOrderQty(minOrderQty)
                                .description(description.isBlank() ? null : description.trim())
                                .isActive(true)
                                .build();
                        successCount++;
                    }

                    batch.add(product);

                    // ── Flush batch every BATCH_SIZE rows ─────────────────────
                    if (batch.size() >= BATCH_SIZE) {
                        vendorProductRepository.saveAll(batch);
                        batch.clear();
                    }

                } catch (Exception e) {
                    errors.add("Row " + (rowIdx + 1) + ": Unexpected error — " + e.getMessage());
                    failedCount++;
                }
            }

            // Flush remaining
            if (!batch.isEmpty()) {
                vendorProductRepository.saveAll(batch);
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to read Excel file: " + e.getMessage(), e);
        }

        String status;
        if (failedCount == 0 && totalRows > 0) status = "SUCCESS";
        else if (successCount + updatedCount > 0) status = "PARTIAL";
        else status = "FAILED";

        log.info("Vendor product bulk import: total={} success={} updated={} failed={} status={}",
                totalRows, successCount, updatedCount, failedCount, status);

        return VendorBulkImportResponseDTO.builder()
                .fileName(file.getOriginalFilename())
                .totalRows(totalRows)
                .successCount(successCount)
                .updatedCount(updatedCount)
                .failedCount(failedCount)
                .skippedCount(skippedCount)
                .status(status)
                .processedAt(LocalDateTime.now())
                .errors(errors)
                .skippedSkus(skippedSkus)
                .vendorCodesNotFound(vendorCodesNotFound)
                .build();
    }

    // ── Template Download ────────────────────────────────────────────────────────

    /**
     * Generate a downloadable template Excel file with headers + sample row.
     * Same style as the product bulk upload template.
     */
    public byte[] generateTemplate() {
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook =
                     new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet("Vendor Products");

            // ── Row 0: Legend banner ──────────────────────────────────────────
            Row legend = sheet.createRow(0);
            Cell legendCell = legend.createCell(0);
            legendCell.setCellValue(
                "VENDOR PRODUCT BULK UPLOAD TEMPLATE  |  " +
                "Row 1: Legend (do not edit)  |  " +
                "Row 2: Headers (do not edit)  |  " +
                "Row 3: Sample (DELETE before upload)  |  " +
                "Row 4+: Your data");

            CellStyle legendStyle = workbook.createCellStyle();
            legendStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.DARK_BLUE.getIndex());
            legendStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font legendFont = workbook.createFont();
            legendFont.setColor(org.apache.poi.ss.usermodel.IndexedColors.WHITE.getIndex());
            legendFont.setBold(true);
            legendStyle.setFont(legendFont);
            legendCell.setCellStyle(legendStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, TEMPLATE_HEADERS.length - 1));

            // ── Row 1: Column headers ─────────────────────────────────────────
            Row headerRow = sheet.createRow(1);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setColor(org.apache.poi.ss.usermodel.IndexedColors.WHITE.getIndex());
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            for (int i = 0; i < TEMPLATE_HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(TEMPLATE_HEADERS[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5000);
            }

            // ── Row 2: Sample data ────────────────────────────────────────────
            Row sampleRow = sheet.createRow(2);
            CellStyle sampleStyle = workbook.createCellStyle();
            sampleStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.LIGHT_BLUE.getIndex());
            sampleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font sampleFont = workbook.createFont();
            sampleFont.setItalic(true);
            sampleStyle.setFont(sampleFont);

            String[] sampleData = {
                "24AAAAA0000A1Z5",      // GSTIN
                "Amul Full Cream Milk", // Product Name
                "AMU-MILK-500",         // Vendor SKU
                "22.00",                // Purchase Price
                "PCS",                  // Unit of Measure
                "500ml",                // Pack Size
                "5",                    // GST %
                "0401",                 // HSN Code
                "Amul",                 // Brand
                "Dairy",                // Category
                "100",                  // Min Order Qty
                "Full cream milk 500ml tetra pack"  // Description
            };

            for (int i = 0; i < sampleData.length; i++) {
                Cell cell = sampleRow.createCell(i);
                cell.setCellValue(sampleData[i]);
                cell.setCellStyle(sampleStyle);
            }

            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate template: " + e.getMessage(), e);
        }
    }

    // ── Private Helpers ──────────────────────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file provided. Please upload an Excel (.xlsx) file.");
        }
        String name = file.getOriginalFilename();
        if (name == null || (!name.endsWith(".xlsx") && !name.endsWith(".xls"))) {
            throw new IllegalArgumentException("Only Excel files (.xlsx, .xls) are supported.");
        }
        // 10 MB max
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds 10 MB limit.");
        }
    }

    private String getCellString(Row row, int colIdx) {
        Cell cell = row.getCell(colIdx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toString();
                }
                double val = cell.getNumericCellValue();
                // Return as integer string if whole number
                return val == Math.floor(val) ? String.valueOf((long) val) : String.valueOf(val);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    return cell.getStringCellValue().trim();
                }
            default:
                return "";
        }
    }

    private boolean isRowEmpty(Row row) {
        for (int i = COL_GSTIN; i <= COL_DESCRIPTION; i++) {
            Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String val = getCellString(row, i);
                if (!val.isBlank()) return false;
            }
        }
        return true;
    }
}
