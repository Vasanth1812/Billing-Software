package com.Billing_System.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single row in the Vendor Performance Report table.
 * KPIs are computed from GRN, invoice, and scorecard data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceReportRowDTO {

    private String id;
    private String vendorCode;
    private String vendorName;
    private String tier;
    private int overallScore;
    private int onTimeDelivery;       // % of GRNs delivered on-time
    private int qualityScore;         // % accepted vs total received
    private int fulfillmentRate;      // % accepted vs ordered
    private int gstCompliance;
    private int totalPOs;
    private int totalGRNs;
    private String status;
}
