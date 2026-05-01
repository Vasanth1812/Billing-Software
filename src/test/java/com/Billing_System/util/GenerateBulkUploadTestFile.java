package com.Billing_System.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Run this class once to generate a BillPro-format test XLSX file.
 *
 * HOW TO RUN IN INTELLIJ:
 *   Right-click this file → Run 'GenerateBulkUploadTestFile.main()'
 *
 * OUTPUT:
 *   D:\Billing-System\test_bulk_upload.xlsx
 *
 * FORMAT (matches BillPro Products template exactly):
 *   Row 1: Legend banner (colour-coded) — DO NOT EDIT
 *   Row 2: Column headers (dark blue)   — DO NOT EDIT
 *   Row 3: Sample data (blue italic)    — DELETE before uploading
 *   Row 4+: Actual data (20 dummy rows + 2 intentional error rows for testing)
 */
public class GenerateBulkUploadTestFile {

    private static final String OUTPUT_PATH = "D:\\Billing-System\\test_bulk_upload.xlsx";

    // Column headers — must match BillPro Products template exactly
    private static final String[] HEADERS = {
            "Product Name",        // A - COL 0  - REQUIRED (yellow)
            "SKU / Barcode",       // B - COL 1  - REQUIRED (yellow)
            "Category",            // C - COL 2  - REQUIRED (yellow)
            "Unit of Measure",     // D - COL 3  - REQUIRED (yellow)
            "Purchase Rate (₹)",   // E - COL 4  - REQUIRED (yellow)
            "MRP / Selling Price", // F - COL 5  - REQUIRED (yellow)
            "GST %",               // G - COL 6  - REQUIRED (yellow)
            "HSN Code",            // H - COL 7  - REQUIRED (yellow)
            "Opening Stock Qty",   // I - COL 8  - optional
            "Min Stock Level",     // J - COL 9  - optional
            "Product Description", // K - COL 10 - optional
            "Brand",               // L - COL 11 - optional
            "Supplier Name",       // M - COL 12 - optional
            "Expiry / Shelf Life", // N - COL 13 - optional
            "Active (YES/NO)"      // O - COL 14 - optional
    };

    // Dummy data: { name, sku, category, unit, purchaseRate, mrp, gstRate, hsnCode,
    //               openingStock, minStock, description, brand, supplier, expiry, active }
    private static final String[][] PRODUCTS = {
            {"Amul Butter 500g",      "SKU-1001", "Dairy",       "Pcs", "215", "250", "12", "0405", "50",  "10", "Fresh salted butter", "Amul",           "Gujarat Coop",  "6 months",  "YES"},
            {"Amul Milk 1L",          "SKU-1002", "Dairy",       "Ltr", "52",  "60",  "5",  "0401", "200", "30", "Full cream milk",      "Amul",           "Gujarat Coop",  "2 days",    "YES"},
            {"Tata Salt 1Kg",         "SKU-1003", "Grocery",     "Kg",  "18",  "22",  "5",  "2501", "100", "20", "Iodised salt",         "Tata",           "Tata Consumer", "24 months", "YES"},
            {"Fortune Sunflower Oil 1L","SKU-1004","Grocery",    "Ltr", "110", "135", "5",  "1512", "80",  "15", "Refined sunflower oil", "Fortune",        "Adani Wilmar",  "12 months", "YES"},
            {"Basmati Rice 1Kg",      "SKU-1005", "Grocery",     "Kg",  "85",  "110", "5",  "1006", "150", "25", "Premium basmati rice",  "India Gate",     "KRBL Ltd",      "18 months", "YES"},
            {"Britannia Biscuit 100g","SKU-1006", "Snacks",      "Pcs", "20",  "25",  "18", "1905", "200", "40", "Good Day cashew",       "Britannia",      "Britannia Ind", "6 months",  "YES"},
            {"Maggi Noodles 70g",     "SKU-1007", "Snacks",      "Pcs", "12",  "15",  "18", "1902", "300", "50", "Masala flavour",        "Maggi",          "Nestle India",  "12 months", "YES"},
            {"Colgate Toothpaste 200g","SKU-1008","Personal Care","Pcs", "95",  "120", "18", "3306", "80",  "15", "Strong teeth formula",  "Colgate",        "Colgate-Palmo", "24 months", "YES"},
            {"Dettol Soap 100g",      "SKU-1009", "Personal Care","Pcs", "38",  "50",  "18", "3401", "120", "20", "Antibacterial soap",    "Dettol",         "Reckitt",       "24 months", "YES"},
            {"Surf Excel 500g",       "SKU-1010", "Household",   "Pcs", "85",  "105", "18", "3402", "60",  "10", "Quick wash detergent",  "Surf Excel",     "HUL",           "24 months", "YES"},
            {"Parle-G Biscuit 200g",  "SKU-1011", "Snacks",      "Pcs", "15",  "20",  "18", "1905", "500", "80", "Classic glucose biscuit","Parle",         "Parle Products","6 months",  "YES"},
            {"Coca Cola 500ml",       "SKU-1012", "Beverages",   "Pcs", "22",  "30",  "28", "2202", "100", "20", "Chilled cola drink",    "Coca Cola",      "HCCB",          "6 months",  "YES"},
            {"Lay's Chips 26g",       "SKU-1013", "Snacks",      "Pcs", "18",  "20",  "12", "2008", "200", "30", "Classic salted chips",  "Lays",           "PepsiCo India", "4 months",  "YES"},
            {"Horlicks 200g",         "SKU-1014", "Beverages",   "Pcs", "110", "140", "18", "1901", "50",  "10", "Health drink powder",   "Horlicks",       "HUL",           "18 months", "YES"},
            {"Vim Dish Wash 500ml",   "SKU-1015", "Household",   "Pcs", "55",  "75",  "18", "3402", "70",  "10", "Lemon dishwash liquid",  "Vim",           "HUL",           "24 months", "YES"},
            {"Cadbury Dairy Milk 40g","SKU-1016", "Confectionery","Pcs", "28",  "40",  "18", "1806", "150", "25", "Milk chocolate bar",    "Cadbury",        "Mondelez",      "12 months", "YES"},
            {"Aashirvaad Atta 5Kg",   "SKU-1017", "Grocery",     "Pcs", "210", "260", "5",  "1101", "30",  "5",  "Whole wheat flour",     "Aashirvaad",     "ITC Ltd",       "6 months",  "YES"},
            {"Pepsodent Brush",        "SKU-1018","Personal Care","Pcs", "25",  "35",  "18", "9603", "100", "20", "Soft bristle brush",    "Pepsodent",      "HUL",           "36 months", "YES"},
            {"Lifebuoy Soap 100g",    "SKU-1019", "Personal Care","Pcs", "28",  "40",  "18", "3401", "90",  "15", "Red health soap",       "Lifebuoy",       "HUL",           "24 months", "YES"},
            {"Haldiram Bhujia 200g",  "SKU-1020", "Snacks",      "Pcs", "60",  "80",  "12", "2008", "100", "20", "Crispy sev bhujia",     "Haldirams",      "Haldirams",     "6 months",  "YES"},
            // --- Intentional ERROR rows (for testing error handling) ---
            {"Error Test - Bad GST",  "SKU-ERR1", "Grocery",     "Pcs", "50",  "80",  "15", "1006", "",    "",   "",                      "",               "",              "",          "YES"},
            {"Error Test - MRP Low",  "SKU-ERR2", "Grocery",     "Pcs", "100", "80",  "5",  "1006", "",    "",   "",                      "",               "",              "",          "YES"},
    };

    public static void main(String[] args) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Products");

            // Set column widths for readability
            int[] colWidths = {8000, 4000, 4000, 3500, 4000, 4500, 2500, 3000,
                               4000, 4000, 8000, 3500, 4500, 4000, 4000};
            for (int i = 0; i < colWidths.length; i++) {
                sheet.setColumnWidth(i, colWidths[i]);
            }

            // ── ROW 1: Legend Banner ──────────────────────────────────────
            Row legendRow = sheet.createRow(0);
            legendRow.setHeightInPoints(25);
            Cell legendCell = legendRow.createCell(0);
            legendCell.setCellValue(
                "BillPro Products Bulk Upload Template v1.0 | " +
                "🟡 Yellow = Required | ⬜ White = Optional | " +
                "Delete Row 3 (sample) before uploading | Max 5,000 rows");
            legendCell.setCellStyle(createLegendStyle(workbook));
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, HEADERS.length - 1));

            // ── ROW 2: Column Headers ─────────────────────────────────────
            Row headerRow = sheet.createRow(1);
            headerRow.setHeightInPoints(20);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(i < 8
                        ? createRequiredHeaderStyle(workbook)   // yellow for required
                        : createOptionalHeaderStyle(workbook));  // grey for optional
            }

            // ── ROW 3: Sample Row (blue italic — user must delete before upload) ──
            Row sampleRow = sheet.createRow(2);
            sampleRow.setHeightInPoints(18);
            String[] sample = {"Basmati Rice 1Kg","SKU-0001","Grocery","Kg","42","58","5","1006","20","5","Premium basmati","India Gate","Kohinoor Foods","18 months","YES"};
            for (int i = 0; i < sample.length; i++) {
                Cell cell = sampleRow.createCell(i);
                cell.setCellValue(sample[i]);
                cell.setCellStyle(createSampleStyle(workbook));
            }

            // ── ROWS 4+: Actual dummy product data ────────────────────────
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle errorStyle = createErrorStyle(workbook); // red bg for error rows
            for (int r = 0; r < PRODUCTS.length; r++) {
                Row row = sheet.createRow(r + 3); // starts at row index 3 = Excel row 4
                boolean isErrorRow = r >= PRODUCTS.length - 2; // last 2 are error rows
                for (int c = 0; c < PRODUCTS[r].length; c++) {
                    Cell cell = row.createCell(c);
                    cell.setCellValue(PRODUCTS[r][c]);
                    cell.setCellStyle(isErrorRow ? errorStyle : dataStyle);
                }
            }

            // Save file
            try (FileOutputStream fos = new FileOutputStream(OUTPUT_PATH)) {
                workbook.write(fos);
            }
            System.out.println("✅ Test file generated: " + OUTPUT_PATH);
            System.out.println("   Total rows: " + PRODUCTS.length + " (20 valid + 2 intentional errors)");
            System.out.println("   Upload this file to: POST /api/products/bulk-import");
        }
    }

    // ─── Style Helpers ───────────────────────────────────────────────────────────

    private static CellStyle createLegendStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        XSSFFont font = wb.createFont();
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setBold(true);
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        return style;
    }

    private static CellStyle createRequiredHeaderStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        return style;
    }

    private static CellStyle createOptionalHeaderStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        return style;
    }

    private static CellStyle createSampleStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFFont font = wb.createFont();
        font.setItalic(true);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        return style;
    }

    private static CellStyle createDataStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        return style;
    }

    private static CellStyle createErrorStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(IndexedColors.ROSE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        XSSFFont font = wb.createFont();
        font.setColor(IndexedColors.DARK_RED.getIndex());
        style.setFont(font);
        return style;
    }
}
