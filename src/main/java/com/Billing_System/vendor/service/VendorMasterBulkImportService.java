package com.Billing_System.vendor.service;

import com.Billing_System.entity.User;
import com.Billing_System.repository.UserRepository;
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
 * VendorMasterBulkImportService — Imports vendor master records from XLSX.
 *
 * ═══ Excel Format (Vendor Master Sheet) ═══
 * Row 1: Legend banner — DO NOT EDIT
 * Row 2: Column headers — DO NOT EDIT
 * Row 3: Sample data   — DELETE before upload
 * Row 4+: Actual data
 *
 * Column mapping (0-indexed):
 *  0  - Legal Name         (REQUIRED)
 *  1  - Trade Name         (optional)
 *  2  - GSTIN              (REQUIRED — unique identifier per vendor)
 *  3  - PAN Number         (optional)
 *  4  - Business Type      (REQUIRED — MANUFACTURER / TRADER / SERVICE)
 *  5  - Primary Mobile     (REQUIRED)
 *  6  - Primary Email      (REQUIRED)
 *  7  - GST Reg Type       (optional — REGULAR/COMPOSITION/UNREGISTERED)
 *  8  - Annual Turnover    (optional — BELOW_40L / 40L_1CR / ABOVE_1CR)
 *  9  - Website            (optional)
 * 10  - Notes              (optional)
 *
 * VENDOR DUPLICATES:
 *   - If GSTIN already exists → UPDATE existing vendor (not skipped)
 *   - New vendors → auto-assigned vendor code VND-000001, VND-000002...
 * 
 * PRODUCT DUPLICATES:
 *   - If Vendor SKU exists for this Vendor → UPDATE product
 *   - Otherwise → INSERT new product
 *
 * Max 2,000 rows per file.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VendorMasterBulkImportService {

    private static final int BATCH_SIZE    = 200;
    private static final int MAX_ROWS      = 2000;
    private static final int DATA_START_ROW = 5; // 0-indexed: Row 6 in Excel = index 5

    // Vendor fields (0-10)
    private static final int COL_LEGAL_NAME    = 0;
    private static final int COL_TRADE_NAME    = 1;
    private static final int COL_GSTIN         = 2;
    private static final int COL_PAN           = 3;
    private static final int COL_BUSINESS_TYPE = 4;
    private static final int COL_MOBILE        = 5;
    private static final int COL_EMAIL         = 6;
    private static final int COL_GST_REG_TYPE  = 7;
    private static final int COL_TURNOVER      = 8;
    private static final int COL_WEBSITE       = 9;
    private static final int COL_NOTES         = 10;

    // Product fields (11-21)
    private static final int COL_PRODUCT_NAME   = 11;
    private static final int COL_VENDOR_SKU     = 12;
    private static final int COL_PURCHASE_PRICE = 13;
    private static final int COL_UNIT           = 14;
    private static final int COL_PACK_SIZE      = 15;
    private static final int COL_GST_RATE       = 16;
    private static final int COL_HSN_CODE       = 17;
    private static final int COL_BRAND          = 18;
    private static final int COL_CATEGORY       = 19;
    private static final int COL_MIN_ORDER_QTY  = 20;
    private static final int COL_DESCRIPTION    = 21;

    public static final String[] TEMPLATE_HEADERS = {
            "Legal Name",
            "Trade Name",
            "GSTIN",
            "PAN Number",
            "Business Type",
            "Primary Mobile",
            "Primary Email",
            "GST Reg Type",
            "Annual Turnover Range",
            "Website",
            "Notes",
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

    private static final Set<String> VALID_BUSINESS_TYPES =
            Set.of("MANUFACTURER", "TRADER", "SERVICE", "DISTRIBUTOR", "IMPORTER");

    private static final Set<BigDecimal> VALID_GST_RATES = Set.of(
            BigDecimal.ZERO,
            new BigDecimal("5"),
            new BigDecimal("12"),
            new BigDecimal("18"),
            new BigDecimal("28")
    );

    private final VendorRepository vendorRepository;
    private final VendorProductRepository vendorProductRepository;
    private final UserRepository   userRepository;

    @Transactional
    public VendorBulkImportResponseDTO importFromXlsx(MultipartFile file, UUID uploadedByUserId) {
        validateFile(file);

        User uploadedBy = (uploadedByUserId != null)
                ? userRepository.findById(uploadedByUserId).orElse(null)
                : null;

        // Pre-load ALL existing GSTINs → vendor map for fast lookup
        Map<String, Vendor> gstinCache = new HashMap<>();
        Map<String, Vendor> emailCache = new HashMap<>();
        vendorRepository.findAll().forEach(v -> {
            if (v.getGstin() != null)        gstinCache.put(v.getGstin().toUpperCase(), v);
            if (v.getPrimaryEmail() != null)  emailCache.put(v.getPrimaryEmail().toLowerCase(), v);
        });

        // Current vendor count for auto-code generation
        long vendorCount = vendorRepository.countAllVendors();

        List<String>  errors       = new ArrayList<>();
        List<Vendor>  vendorBatch  = new ArrayList<>(BATCH_SIZE);
        List<VendorProduct> productBatch = new ArrayList<>(BATCH_SIZE);
        
        int vendorSuccessCount = 0;
        int vendorUpdatedCount = 0;
        int productSuccessCount = 0;
        int productUpdatedCount = 0;
        int failedCount  = 0;
        int totalRows    = 0;

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet      = workbook.getSheetAt(0);
            int   lastRowNum = sheet.getLastRowNum();

            if (lastRowNum < DATA_START_ROW) {
                throw new IllegalArgumentException("Excel has no data rows. Add vendor data from row 4 onwards.");
            }
            if (lastRowNum - DATA_START_ROW + 1 > MAX_ROWS) {
                throw new IllegalArgumentException("Max " + MAX_ROWS + " vendors per file.");
            }

            // ── Validate Headers ──────────────────────────────────────────────
            Row headerRow = sheet.getRow(1);
            if (headerRow == null) {
                throw new IllegalArgumentException("Invalid template format. Header row is missing.");
            }
            
            // Check first few critical columns to ensure it's the right template
            String col0 = getCellString(headerRow, 0);
            String col2 = getCellString(headerRow, 2);
            String col4 = getCellString(headerRow, 4);
            
            if (!"Legal Name".equalsIgnoreCase(col0) || 
                !"GSTIN".equalsIgnoreCase(col2) || 
                !"Business Type".equalsIgnoreCase(col4)) {
                throw new IllegalArgumentException(
                    "Invalid template format detected. " +
                    "It looks like you uploaded the wrong Excel file (e.g., Store Products template). " +
                    "Please click 'Download Template' to get the correct Vendor Bulk Import template."
                );
            }

            for (int rowIdx = DATA_START_ROW; rowIdx <= lastRowNum; rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null || isRowEmpty(row)) continue;

                totalRows++;
                int excelRow = rowIdx + 1;

                try {
                    // ── Read fields ───────────────────────────────────────────
                    String legalName    = getCellString(row, COL_LEGAL_NAME);
                    String tradeName    = getCellString(row, COL_TRADE_NAME);
                    String gstin        = getCellString(row, COL_GSTIN).toUpperCase().replace(" ", "");
                    String pan          = getCellString(row, COL_PAN).toUpperCase();
                    String businessType = getCellString(row, COL_BUSINESS_TYPE).toUpperCase().trim();
                    String mobile       = getCellString(row, COL_MOBILE);
                    String email        = getCellString(row, COL_EMAIL).toLowerCase().trim();
                    String gstRegType   = getCellString(row, COL_GST_REG_TYPE);
                    String turnover     = getCellString(row, COL_TURNOVER);
                    String website      = getCellString(row, COL_WEBSITE);
                    String notes        = getCellString(row, COL_NOTES);

                    // ── Validate required ─────────────────────────────────────
                    List<String> missing = new ArrayList<>();
                    if (legalName.isBlank())    missing.add("Legal Name");
                    if (gstin.isBlank())         missing.add("GSTIN");
                    if (businessType.isBlank())  missing.add("Business Type");
                    if (mobile.isBlank())        missing.add("Primary Mobile");
                    if (email.isBlank())         missing.add("Primary Email");

                    if (!missing.isEmpty()) {
                        errors.add("Row " + excelRow + ": Missing: " + String.join(", ", missing));
                        failedCount++;
                        continue;
                    }

                    // ── Validate business type ────────────────────────────────
                    if (!VALID_BUSINESS_TYPES.contains(businessType)) {
                        errors.add("Row " + excelRow + ": Invalid Business Type '" + businessType
                                + "'. Valid: " + VALID_BUSINESS_TYPES);
                        failedCount++;
                        continue;
                    }

                    // ── Upsert by GSTIN ───────────────────────────────────────
                    Vendor vendorObjToUse = gstinCache.get(gstin);

                    if (vendorObjToUse != null) {
                        // UPDATE existing vendor
                        vendorObjToUse.setLegalName(legalName.trim());
                        vendorObjToUse.setTradeName(tradeName.isBlank() ? null : tradeName.trim());
                        vendorObjToUse.setPanNumber(pan.isBlank() ? null : pan);
                        vendorObjToUse.setBusinessType(businessType);
                        vendorObjToUse.setPrimaryMobile(mobile.trim());
                        vendorObjToUse.setPrimaryEmail(email);
                        vendorObjToUse.setGstRegistrationType(gstRegType.isBlank() ? null : gstRegType.trim());
                        vendorObjToUse.setAnnualTurnoverRange(turnover.isBlank() ? null : turnover.trim());
                        vendorObjToUse.setWebsite(website.isBlank() ? null : website.trim());
                        vendorObjToUse.setNotes(notes.isBlank() ? null : notes.trim());
                        vendorObjToUse.setUpdatedAt(LocalDateTime.now());
                        vendorBatch.add(vendorObjToUse);
                        vendorUpdatedCount++;
                    } else {
                        // INSERT new vendor — check email uniqueness
                        if (emailCache.containsKey(email)) {
                            errors.add("Row " + excelRow + ": Email '" + email + "' already used by another vendor.");
                            failedCount++;
                            continue;
                        }

                        vendorCount++;
                        String vendorCode = String.format("VND-%06d", vendorCount);

                        vendorObjToUse = Vendor.builder()
                                .vendorCode(vendorCode)
                                .legalName(legalName.trim())
                                .tradeName(tradeName.isBlank() ? null : tradeName.trim())
                                .gstin(gstin)
                                .panNumber(pan.isBlank() ? null : pan)
                                .businessType(businessType)
                                .primaryMobile(mobile.trim())
                                .primaryEmail(email)
                                .gstRegistrationType(gstRegType.isBlank() ? null : gstRegType.trim())
                                .annualTurnoverRange(turnover.isBlank() ? null : turnover.trim())
                                .website(website.isBlank() ? null : website.trim())
                                .notes(notes.isBlank() ? null : notes.trim())
                                .kycStatus("PENDING")
                                .complianceStatus("PENDING")
                                .onboardingStage("CATEGORY_MANAGER_REVIEW")
                                .createdBy(uploadedBy)
                                .build();

                        vendorBatch.add(vendorObjToUse);
                        // Update caches to catch intra-file duplicates
                        gstinCache.put(gstin, vendorObjToUse);
                        emailCache.put(email, vendorObjToUse);
                        vendorSuccessCount++;
                    }

                    // Flush vendor batch early if needed so products can attach to saved vendors
                    if (vendorBatch.size() >= BATCH_SIZE) {
                        vendorRepository.saveAll(vendorBatch);
                        vendorBatch.clear();
                    }

                    // ── Parse Product details if any ──────────────────────────
                    String productName  = getCellString(row, COL_PRODUCT_NAME);
                    if (!productName.isBlank()) {
                        String vendorSku = getCellString(row, COL_VENDOR_SKU);
                        String priceStr  = getCellString(row, COL_PURCHASE_PRICE);
                        String unit      = getCellString(row, COL_UNIT);
                        String gstStr    = getCellString(row, COL_GST_RATE);

                        List<String> missingProd = new ArrayList<>();
                        if (vendorSku.isBlank())   missingProd.add("Vendor SKU");
                        if (priceStr.isBlank())    missingProd.add("Purchase Price");
                        if (unit.isBlank())        missingProd.add("Unit of Measure");
                        if (gstStr.isBlank())      missingProd.add("GST %");

                        if (!missingProd.isEmpty()) {
                            errors.add("Row " + excelRow + ": Product missing required fields: " + String.join(", ", missingProd));
                        } else {
                            // Validate Price
                            BigDecimal purchasePrice = null;
                            try {
                                purchasePrice = new BigDecimal(priceStr.replace(",", "").trim());
                                if (purchasePrice.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException();
                            } catch (Exception e) {
                                errors.add("Row " + excelRow + ": Invalid Purchase Price '" + priceStr + "'");
                            }

                            // Validate GST
                            BigDecimal gstRate = null;
                            try {
                                gstRate = new BigDecimal(gstStr.replace("%", "").trim()).stripTrailingZeros();
                                if (!VALID_GST_RATES.contains(gstRate)) throw new NumberFormatException();
                            } catch (Exception e) {
                                errors.add("Row " + excelRow + ": Invalid GST rate '" + gstStr + "'");
                            }

                            if (purchasePrice != null && gstRate != null) {
                                String packSize    = getCellString(row, COL_PACK_SIZE);
                                String hsnCode     = getCellString(row, COL_HSN_CODE);
                                String brand       = getCellString(row, COL_BRAND);
                                String category    = getCellString(row, COL_CATEGORY);
                                String minQtyStr   = getCellString(row, COL_MIN_ORDER_QTY);
                                String description = getCellString(row, COL_DESCRIPTION);

                                BigDecimal minOrderQty = null;
                                if (!minQtyStr.isBlank()) {
                                    try { minOrderQty = new BigDecimal(minQtyStr.replace(",", "").trim()); } catch (Exception ignored) {}
                                }

                                // We must ensure the vendor has an ID before mapping product in the DB, 
                                // so we save the vendor immediately if it is transient.
                                if (vendorObjToUse.getId() == null) {
                                    vendorObjToUse = vendorRepository.save(vendorObjToUse);
                                    // Remove from batch to avoid duplicate saving
                                    vendorBatch.remove(vendorBatch.size() - 1);
                                }

                                Optional<VendorProduct> existingProduct = vendorProductRepository.findByVendorIdAndVendorSku(vendorObjToUse.getId(), vendorSku.trim());

                                VendorProduct product;
                                if (existingProduct.isPresent()) {
                                    product = existingProduct.get();
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
                                    productUpdatedCount++;
                                } else {
                                    product = VendorProduct.builder()
                                            .vendor(vendorObjToUse)
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
                                    productSuccessCount++;
                                }

                                productBatch.add(product);
                                if (productBatch.size() >= BATCH_SIZE) {
                                    vendorProductRepository.saveAll(productBatch);
                                    productBatch.clear();
                                }
                            }
                        }
                    }

                } catch (Exception e) {
                    errors.add("Row " + (rowIdx + 1) + ": Error — " + e.getMessage());
                    failedCount++;
                }
            }

            if (!vendorBatch.isEmpty()) vendorRepository.saveAll(vendorBatch);
            if (!productBatch.isEmpty()) vendorProductRepository.saveAll(productBatch);

        } catch (IOException e) {
            throw new RuntimeException("Failed to read Excel: " + e.getMessage(), e);
        }

        int combinedSuccessCount = vendorSuccessCount + productSuccessCount;
        int combinedUpdatedCount = vendorUpdatedCount + productUpdatedCount;
        
        String status = failedCount == 0 && totalRows > 0 ? "SUCCESS"
                : combinedSuccessCount + combinedUpdatedCount > 0 ? "PARTIAL"
                : "FAILED";

        log.info("Vendor master bulk import: total={} new(v={} p={}) updated(v={} p={}) failed={} status={}",
                totalRows, vendorSuccessCount, productSuccessCount, vendorUpdatedCount, productUpdatedCount, failedCount, status);

        return VendorBulkImportResponseDTO.builder()
                .fileName(file.getOriginalFilename())
                .totalRows(totalRows)
                .successCount(combinedSuccessCount)
                .updatedCount(combinedUpdatedCount)
                .failedCount(failedCount)
                .skippedCount(0)
                .status(status)
                .processedAt(LocalDateTime.now())
                .errors(errors)
                .skippedSkus(List.of())
                .vendorCodesNotFound(List.of())
                .build();
    }

    /** Generate downloadable Vendor Master template */
    public byte[] generateTemplate() {
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb =
                     new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {

            Sheet sheet = wb.createSheet("Vendor Master");

            // Row 0 — Legend
            Row legend = sheet.createRow(0);
            Cell lc = legend.createCell(0);
            lc.setCellValue("VENDOR MASTER BULK UPLOAD  |  Row 1: Legend  |  Row 2: Headers  |  Row 3-5: Samples (DELETE before uploading)  |  Row 6+: Data");
            CellStyle ls = wb.createCellStyle();
            ls.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            ls.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font lf = wb.createFont();
            lf.setColor(IndexedColors.WHITE.getIndex());
            lf.setBold(true);
            ls.setFont(lf);
            lc.setCellStyle(ls);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, TEMPLATE_HEADERS.length - 1));

            // Row 1 — Headers
            Row headerRow = sheet.createRow(1);
            CellStyle hs = wb.createCellStyle();
            hs.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            hs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font hf = wb.createFont();
            hf.setColor(IndexedColors.WHITE.getIndex());
            hf.setBold(true);
            hs.setFont(hf);
            for (int i = 0; i < TEMPLATE_HEADERS.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(TEMPLATE_HEADERS[i]);
                c.setCellStyle(hs);
                sheet.setColumnWidth(i, 5500);
            }

            // Row 2 — Sample 1 (Amul Milk)
            Row sample1 = sheet.createRow(2);
            CellStyle ss = wb.createCellStyle();
            ss.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            ss.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font sf = wb.createFont(); sf.setItalic(true);
            ss.setFont(sf);
            String[] sampleData1 = {
                "Amul Dairy Products Pvt Ltd", "Amul", "24AAAAA0000A1Z5", "AAAAA0000A",
                "MANUFACTURER", "9876543210", "procurement@amul.com",
                "REGULAR", "ABOVE_1CR", "www.amul.com", "Bulk import",
                "Amul Full Cream Milk", "AMU-MILK-500", "22.00", "PCS", "500ml", "5", "0401", "Amul", "Dairy", "100", "Full cream milk tetra pack"
            };
            for (int i = 0; i < sampleData1.length; i++) {
                Cell c = sample1.createCell(i); c.setCellValue(sampleData1[i]); c.setCellStyle(ss);
            }

            // Row 3 — Sample 2 (Amul Butter - Same Vendor, different product)
            Row sample2 = sheet.createRow(3);
            String[] sampleData2 = {
                "Amul Dairy Products Pvt Ltd", "Amul", "24AAAAA0000A1Z5", "AAAAA0000A",
                "MANUFACTURER", "9876543210", "procurement@amul.com",
                "REGULAR", "ABOVE_1CR", "www.amul.com", "Bulk import",
                "Amul Salted Butter", "AMU-BUTTER-100", "56.00", "PCS", "100g", "12", "0405", "Amul", "Dairy", "50", "Salted table butter"
            };
            for (int i = 0; i < sampleData2.length; i++) {
                Cell c = sample2.createCell(i); c.setCellValue(sampleData2[i]); c.setCellStyle(ss);
            }

            // Row 4 — Sample 3 (Britannia Marie Gold - Different Vendor, different product)
            Row sample3 = sheet.createRow(4);
            String[] sampleData3 = {
                "Britannia Industries Ltd", "Britannia", "24BBBBB1111B2Z6", "BBBBB1111B",
                "DISTRIBUTOR", "9988776655", "sales@britannia.com",
                "REGULAR", "ABOVE_1CR", "www.britannia.co.in", "Bulk onboarding",
                "Britannia Marie Gold Biscuit", "BRI-MARIE-250", "30.00", "PCS", "250g", "18", "1905", "Britannia", "Biscuits", "200", "Tea time Marie Gold biscuits"
            };
            for (int i = 0; i < sampleData3.length; i++) {
                Cell c = sample3.createCell(i); c.setCellValue(sampleData3[i]); c.setCellStyle(ss);
            }

            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Template generation failed", e);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty())
            throw new IllegalArgumentException("No file provided.");
        String name = file.getOriginalFilename();
        if (name == null || (!name.endsWith(".xlsx") && !name.endsWith(".xls")))
            throw new IllegalArgumentException("Only .xlsx / .xls files supported.");
        if (file.getSize() > 10 * 1024 * 1024)
            throw new IllegalArgumentException("File exceeds 10 MB limit.");
    }

    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                double val = cell.getNumericCellValue();
                return val == Math.floor(val) ? String.valueOf((long) val) : String.valueOf(val);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    private boolean isRowEmpty(Row row) {
        for (int i = COL_LEGAL_NAME; i <= COL_DESCRIPTION; i++) {
            Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (cell != null && cell.getCellType() != CellType.BLANK
                    && !getCellString(row, i).isBlank()) return false;
        }
        return true;
    }
}
