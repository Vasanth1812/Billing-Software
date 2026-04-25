package com.Billing_System.controller;

import com.Billing_System.dto.GstSummaryDTO;
import com.Billing_System.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Adjust as per your security needs
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/gst-summary")
    public ResponseEntity<GstSummaryDTO> getGstSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        // Default to current month if not provided
        if (from == null) {
            from = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth());
        }
        if (to == null) {
            to = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());
        }

        return ResponseEntity.ok(reportService.getGstSummary(from, to));
    }
}
