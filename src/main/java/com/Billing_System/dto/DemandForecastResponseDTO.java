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
public class DemandForecastResponseDTO {
    private KpiDataDTO kpiData;
    private List<ChartDataDTO> chartData;
}
