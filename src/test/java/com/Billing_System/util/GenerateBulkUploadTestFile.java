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
 *   D:\Billing-System\test_bulk_upload_2.xlsx
 *
 * FORMAT (matches BillPro Products template exactly):
 *   Row 1: Legend banner (colour-coded) — DO NOT EDIT
 *   Row 2: Column headers (dark blue)   — DO NOT EDIT
 *   Row 3: Sample data (blue italic)    — DELETE before uploading
 *   Row 4+: Actual data (20 dummy rows + 2 intentional error rows for testing)
 */
public class GenerateBulkUploadTestFile {

    private static final String OUTPUT_PATH = "D:\\Billing-System\\test_bulk_upload_2.xlsx";

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
            {"Nandini Curd 500g",     "SKU-4001", "Dairy",       "Pcs", "32",  "45",  "5",  "0403", "90",  "15", "Set curd pack",          "Nandini",        "KMF Dairy",     "5 days",    "YES"},
            {"Mother Dairy Paneer 200g","SKU-4002","Dairy",      "Pcs", "72",  "95",  "5",  "0406", "60",  "10", "Fresh paneer block",     "Mother Dairy",   "Mother Dairy",  "7 days",    "YES"},
            {"Saffola Gold Oil 1L",   "SKU-4003", "Grocery",     "Ltr", "142", "175", "5",  "1512", "75",  "12", "Blended edible oil",     "Saffola",        "Marico",        "12 months", "YES"},
            {"Daawat Rozana Rice 5Kg", "SKU-4004", "Grocery",    "Bag", "390", "475", "5",  "1006", "40",  "8",  "Everyday basmati rice",  "Daawat",         "LT Foods",      "18 months", "YES"},
            {"Catch Turmeric Powder 200g","SKU-4005","Spices",   "Pcs", "48",  "68",  "5",  "0910", "85",  "15", "Haldi powder",           "Catch",          "DS Group",      "12 months", "YES"},
            {"Everest Garam Masala 100g","SKU-4006","Spices",    "Pcs", "52",  "75",  "5",  "0910", "70",  "12", "Spice mix pouch",        "Everest",        "Everest Foods", "12 months", "YES"},
            {"Kellogg's Corn Flakes 475g","SKU-4007","Breakfast","Pcs", "145", "190", "18", "1904", "45",  "8",  "Breakfast cereal",       "Kelloggs",       "Kelloggs India","9 months",  "YES"},
            {"MTR Poha 500g",         "SKU-4008", "Breakfast",   "Pcs", "46",  "62",  "5",  "1904", "100", "20", "Flattened rice pack",    "MTR",            "MTR Foods",     "9 months",  "YES"},
            {"Real Mixed Fruit Juice 1L","SKU-4009","Beverages", "Ltr", "82",  "115", "12", "2009", "55",  "10", "Fruit juice carton",     "Real",           "Dabur",         "6 months",  "YES"},
            {"Tropicana Orange Juice 1L","SKU-4010","Beverages", "Ltr", "88",  "125", "12", "2009", "50",  "10", "Orange juice carton",    "Tropicana",      "PepsiCo India", "6 months",  "YES"},
            {"Bingo Mad Angles 80g",  "SKU-4011", "Snacks",      "Pcs", "25",  "35",  "12", "2008", "180", "35", "Masala corn snack",      "Bingo",          "ITC Ltd",       "5 months",  "YES"},
            {"Kurkure Masala Munch 90g","SKU-4012","Snacks",     "Pcs", "24",  "35",  "12", "2008", "190", "35", "Crunchy snack pack",     "Kurkure",        "PepsiCo India", "5 months",  "YES"},
            {"Nivea Body Lotion 400ml","SKU-4013","Personal Care","Pcs", "255", "365", "18", "3304", "35",  "6",  "Moisturising lotion",    "Nivea",          "Beiersdorf",    "24 months", "YES"},
            {"Himalaya Face Wash 100ml","SKU-4014","Personal Care","Pcs","92",  "145", "18", "3304", "65",  "10", "Neem face wash",         "Himalaya",       "Himalaya Wellness","24 months","YES"},
            {"Lizol Floor Cleaner 1L", "SKU-4015", "Household",   "Ltr", "128", "190", "18", "3402", "52",  "8",  "Disinfectant cleaner",   "Lizol",          "Reckitt",       "24 months", "YES"},
            {"Harpic Bathroom Cleaner 500ml","SKU-4016","Household","Pcs","74", "110", "18", "3402", "58",  "8",  "Bathroom cleaner",       "Harpic",         "Reckitt",       "24 months", "YES"},
            {"Classmate Notebook 172 Pages","SKU-4017","Stationery","Pcs","38","60",  "12", "4820", "120", "25", "Ruled notebook",         "Classmate",      "ITC Ltd",       "36 months", "YES"},
            {"Cello Butterflow Pen Blue","SKU-4018","Stationery", "Pcs", "6",   "10",  "12", "9608", "300", "50", "Ball pen blue ink",      "Cello",          "BIC Cello",     "36 months", "YES"},
            {"Duracell AA Battery 4pc","SKU-4019", "Electronics", "Pcs", "92",  "150", "18", "8506", "45",  "8",  "Alkaline battery pack",  "Duracell",       "Duracell India","36 months", "YES"},
            {"Philips LED Bulb 9W",   "SKU-4020", "Electronics", "Pcs", "78",  "135", "12", "8539", "70",  "12", "Cool daylight bulb",     "Philips",        "Signify",       "24 months", "YES"},
            // --- Intentional ERROR rows (for testing error handling) ---
            {"Error Test - Bad GST",  "SKU-ERR5", "Grocery",     "Pcs", "50",  "80",  "15", "1006", "",    "",   "",                      "",               "",              "",          "YES"},
            {"Error Test - MRP Low",  "SKU-ERR6", "Grocery",     "Pcs", "100", "80",  "5",  "1006", "",    "",   "",                      "",               "",              "",          "YES"},
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
            String[] sample = {"Sample Product","SKU-4000","Grocery","Pcs","42","58","5","1006","20","5","Sample description","Sample Brand","Sample Supplier","18 months","YES"};
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
