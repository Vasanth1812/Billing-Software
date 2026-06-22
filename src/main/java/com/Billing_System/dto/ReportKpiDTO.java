package com.Billing_System.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * KPI summary for a specific report type — powers the 4 stat cards at the top
 * of each Reports Hub report view.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportKpiDTO {

    private String reportType;
    private List<KpiItem> kpis;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KpiItem {
        private String label;          // "Total Vendors"
        private String value;          // "42" or "₹12.5L"
        private String accent;         // "blue", "emerald", "amber", "rose" (for CSS)
        private Double changePercent;  // trend: +5.2 or -3.1 (nullable)
    }
}
