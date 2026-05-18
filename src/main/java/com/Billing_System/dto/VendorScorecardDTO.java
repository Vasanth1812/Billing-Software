package com.Billing_System.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorScorecardDTO {
    private String vendorId;
    private String vendorName;
    private String tier;
    private int overallScore;

    // Operational Metrics (0-100 values)
    private int onTimeDelivery;
    private int qualityScore;
    private int priceCompetitiveness;
    private int gstCompliance;
    private int responseTime;
    private int fulfillmentRate;

    // Target Benchmark Scores
    private int onTimeBenchmark;
    private int qualityBenchmark;
    private int priceBenchmark;
    private int gstBenchmark;
    private int responseBenchmark;
    private int fulfillmentBenchmark;

    // List of peers for comparative visualizer
    private List<PeerVendorDTO> peers;
}
