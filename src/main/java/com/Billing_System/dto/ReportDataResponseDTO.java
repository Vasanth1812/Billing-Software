package com.Billing_System.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Generic paginated response wrapper for all report data endpoints.
 * Contains KPI summary, paginated rows, and pagination metadata.
 *
 * @param <T> the row DTO type (VendorReportRowDTO, POReportRowDTO, etc.)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportDataResponseDTO<T> {

    private ReportKpiDTO summary;
    private List<T> data;
    private PaginationMeta pagination;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaginationMeta {
        private int page;
        private int pageSize;
        private long totalRecords;
        private int totalPages;
        private boolean hasNext;
        private boolean hasPrevious;
    }
}
