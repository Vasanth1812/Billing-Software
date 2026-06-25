package com.Billing_System.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KpiDataDTO {
    private Double daysOfCover;
    private Integer safetyStock;
    private Double forecastAccuracy;
}
