package com.Billing_System.vendor.service;

import com.Billing_System.entity.User;
import com.Billing_System.repository.UserRepository;
import com.Billing_System.vendor.dto.VendorBulkImportResponseDTO;
import com.Billing_System.vendor.entity.Vendor;
import com.Billing_System.vendor.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
 * DUPLICATE GSTIN BEHAVIOUR:
 *   - If GSTIN already exists → UPDATE existing vendor (not skipped)
 *   - New vendors → auto-assigned vendor code VND-000001, VND-000002...
 *
 * Max 2,000 vendors per file.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VendorMasterBulkImportService {

    private static final int BATCH_SIZE    = 200;
    private static final int MAX_ROWS      = 2000;
    private static final int DATA_START_ROW = 3;

    // Column indices (0-based)
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
            "Notes"
    };

    private static final Set<String> VALID_BUSINESS_TYPES =
            Set.of("MANUFACTURER", "TRADER", "SERVICE", "DISTRIBUTOR", "IMPORTER");

    private final VendorRepository vendorRepository;
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

        List<String>  errors     = new ArrayList<>();
        List<Vendor>  batch      = new ArrayList<>(BATCH_SIZE);
        int successCount = 0;
        int updatedCount = 0;
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
                    Vendor existing = gstinCache.get(gstin);

                    if (existing != null) {
                        // UPDATE existing vendor
                        existing.setLegalName(legalName.trim());
                        existing.setTradeName(tradeName.isBlank() ? null : tradeName.trim());
                        existing.setPanNumber(pan.isBlank() ? null : pan);
                        existing.setBusinessType(businessType);
                        existing.setPrimaryMobile(mobile.trim());
                        existing.setPrimaryEmail(email);
                        existing.setGstRegistrationType(gstRegType.isBlank() ? null : gstRegType.trim());
                        existing.setAnnualTurnoverRange(turnover.isBlank() ? null : turnover.trim());
                        existing.setWebsite(website.isBlank() ? null : website.trim());
                        existing.setNotes(notes.isBlank() ? null : notes.trim());
                        existing.setUpdatedAt(LocalDateTime.now());
                        batch.add(existing);
                        updatedCount++;
                    } else {
                        // INSERT new vendor — check email uniqueness
                        if (emailCache.containsKey(email)) {
                            errors.add("Row " + excelRow + ": Email '" + email + "' already used by another vendor.");
                            failedCount++;
                            continue;
                        }

                        vendorCount++;
                        String vendorCode = String.format("VND-%06d", vendorCount);

                        Vendor newVendor = Vendor.builder()
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

                        batch.add(newVendor);
                        // Update caches to catch intra-file duplicates
                        gstinCache.put(gstin, newVendor);
                        emailCache.put(email, newVendor);
                        successCount++;
                    }

                    if (batch.size() >= BATCH_SIZE) {
                        vendorRepository.saveAll(batch);
                        batch.clear();
                    }

                } catch (Exception e) {
                    errors.add("Row " + (rowIdx + 1) + ": Error — " + e.getMessage());
                    failedCount++;
                }
            }

            if (!batch.isEmpty()) vendorRepository.saveAll(batch);

        } catch (IOException e) {
            throw new RuntimeException("Failed to read Excel: " + e.getMessage(), e);
        }

        String status = failedCount == 0 && totalRows > 0 ? "SUCCESS"
                : successCount + updatedCount > 0 ? "PARTIAL"
                : "FAILED";

        log.info("Vendor master bulk import: total={} new={} updated={} failed={} status={}",
                totalRows, successCount, updatedCount, failedCount, status);

        return VendorBulkImportResponseDTO.builder()
                .fileName(file.getOriginalFilename())
                .totalRows(totalRows)
                .successCount(successCount)
                .updatedCount(updatedCount)
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
            lc.setCellValue("VENDOR MASTER BULK UPLOAD  |  Row 1: Legend  |  Row 2: Headers  |  Row 3: Sample (DELETE)  |  Row 4+: Data");
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

            // Row 2 — Sample
            Row sample = sheet.createRow(2);
            CellStyle ss = wb.createCellStyle();
            ss.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            ss.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font sf = wb.createFont(); sf.setItalic(true);
            ss.setFont(sf);
            String[] sampleData = {
                "Amul Dairy Products Pvt Ltd", "Amul", "24AAAAA0000A1Z5", "AAAAA0000A",
                "MANUFACTURER", "9876543210", "procurement@amul.com",
                "REGULAR", "ABOVE_1CR", "www.amul.com", "Bulk import"
            };
            for (int i = 0; i < sampleData.length; i++) {
                Cell c = sample.createCell(i); c.setCellValue(sampleData[i]); c.setCellStyle(ss);
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
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double val = cell.getNumericCellValue();
                yield val == Math.floor(val) ? String.valueOf((long) val) : String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private boolean isRowEmpty(Row row) {
        for (int i = COL_LEGAL_NAME; i <= COL_NOTES; i++) {
            Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (cell != null && cell.getCellType() != CellType.BLANK
                    && !getCellString(row, i).isBlank()) return false;
        }
        return true;
    }
}
