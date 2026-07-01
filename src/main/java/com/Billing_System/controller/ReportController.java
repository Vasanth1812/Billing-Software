package com.Billing_System.controller;

import com.Billing_System.dto.*;
import com.Billing_System.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * Report Controller — serves both the existing GST summary endpoint
 * and the new Reports Hub APIs for the Vendor Management frontend.
 *
 * Endpoints:
 *   GET  /api/reports/gst-summary    → Existing GST slab report
 *   GET  /api/reports/catalog        → List of available report types
 *   GET  /api/reports/kpis           → KPI summary cards for a report type
 *   POST /api/reports/data           → Paginated report data table
 *   POST /api/reports/export         → Download Excel file
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReportController {

    private final ReportService reportService;

    // ═══════════════════════════════════════════════════════════════════════════════
    // EXISTING ENDPOINT — GST Summary
    // ═══════════════════════════════════════════════════════════════════════════════

    @GetMapping("/gst-summary")
    public ResponseEntity<GstSummaryDTO> getGstSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        if (from == null) {
            from = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth());
        }
        if (to == null) {
            to = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());
        }

        return ResponseEntity.ok(reportService.getGstSummary(from, to));
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // NEW — REPORTS HUB ENDPOINTS
    // ═══════════════════════════════════════════════════════════════════════════════

    @GetMapping("/stock-movement")
    public ResponseEntity<List<StockMovementDTO>> getStockMovement(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(reportService.getStockMovementReport(from, to, search));
    }

    @GetMapping("/fast-moving")
    public ResponseEntity<List<FastMovingDTO>> getFastMovingProducts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.getFastMovingProducts(from, to));
    }

    @GetMapping("/dead-stock")
    public ResponseEntity<List<DeadStockDTO>> getDeadStockProducts(
            @RequestParam(defaultValue = "30") int daysThreshold) {
        return ResponseEntity.ok(reportService.getDeadStockProducts(daysThreshold));
    }

    @GetMapping("/profit-margin")
    public ResponseEntity<List<ProfitMarginDTO>> getProfitMarginProducts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.getProfitMarginAnalysis(from, to));
    }

    @GetMapping("/supplier-purchase")
    public ResponseEntity<List<SupplierPurchaseDTO>> getSupplierPurchases(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String supplier) {
        return ResponseEntity.ok(reportService.getSupplierPurchases(from, to, supplier));
    }

    @GetMapping("/gst-sales")
    public ResponseEntity<List<GstReportDTO>> getGstSales(
            @RequestParam int month,
            @RequestParam int year) {
        return ResponseEntity.ok(reportService.getGstSales(month, year));
    }

    @GetMapping("/gst-purchases")
    public ResponseEntity<List<GstReportDTO>> getGstPurchases(
            @RequestParam int month,
            @RequestParam int year) {
        return ResponseEntity.ok(reportService.getGstPurchases(month, year));
    }

    /**
     * GET /api/reports/catalog
     * Returns the list of all report types with their available filters and export formats.
     * Used by the Reports Hub sidebar to render report categories.
     */
    @GetMapping("/catalog")
    public ResponseEntity<List<ReportCatalogDTO>> getReportCatalog() {
        return ResponseEntity.ok(reportService.getReportCatalog());
    }

    /**
     * GET /api/reports/kpis?type=master&timePeriod=MONTH
     * Returns the 4 KPI summary cards for a specific report type.
     * These appear at the top of each report view.
     */
    @GetMapping("/kpis")
    public ResponseEntity<ReportKpiDTO> getReportKpis(
            @RequestParam String type,
            @RequestParam(defaultValue = "MONTH") String timePeriod) {
        return ResponseEntity.ok(reportService.getReportKpis(type, timePeriod));
    }

    /**
     * POST /api/reports/data?page=0&size=5
     * Returns paginated report data including KPI summary and table rows.
     * Body: { reportType, searchQuery, statusFilter, sortBy, sortDirection, timePeriod }
     */
    @PostMapping("/data")
    public ResponseEntity<ReportDataResponseDTO<?>> getReportData(
            @RequestBody GenerateReportRequestDTO request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        return ResponseEntity.ok(reportService.getReportData(request, page, size));
    }

    /**
     * POST /api/reports/export
     * Generates and downloads an Excel file containing the full report data.
     * Body: { reportType, format, searchQuery, statusFilter, timePeriod }
     */
    @PostMapping("/export")
    public ResponseEntity<byte[]> exportReport(@RequestBody GenerateReportRequestDTO request) {
        String format = request.getFormat() != null ? request.getFormat().toUpperCase() : "EXCEL";
        
        byte[] bytes = reportService.exportReportAsExcel(request);

        String filename = request.getReportType() + "_report_" + LocalDate.now() + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }
}
