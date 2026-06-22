package com.Billing_System.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Describes a single report type available in the Reports Hub catalog.
 * Sent to the frontend so it can render the sidebar + filter controls dynamically.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportCatalogDTO {

    private String key;            // "master", "po", "performance", "payables"
    private String name;           // "Vendor Master Report"
    private String description;    // Short description for the UI card
    private String category;       // "VENDOR", "PROCUREMENT", "FINANCE"
    private List<ReportParamDTO> parameters;   // Available filter params
    private List<String> exportFormats;        // ["PDF", "EXCEL", "CSV"]

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportParamDTO {
        private String key;        // "dateFrom"
        private String label;      // "From Date"
        private String type;       // "DATE", "SELECT", "TEXT", "MULTI_SELECT"
        private boolean required;
        private List<String> options;  // For SELECT types
    }
}
