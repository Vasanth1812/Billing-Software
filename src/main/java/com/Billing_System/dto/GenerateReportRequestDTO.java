package com.Billing_System.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Inbound request for fetching report data or generating an export.
 * Captures all filter, sort, and pagination preferences from the frontend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateReportRequestDTO {

    private String reportType;       // "master", "po", "performance", "payables"
    private String format;           // "PDF", "EXCEL", "CSV" (only for export)
    private String timePeriod;       // "TODAY", "WEEK", "MONTH", "QUARTER", "YEAR"
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private String statusFilter;     // "all", "active", "pending", "blocked"
    private String searchQuery;
    private String sortBy;           // "name", "id", "rating", "status"
    private String sortDirection;    // "asc", "desc"
}
