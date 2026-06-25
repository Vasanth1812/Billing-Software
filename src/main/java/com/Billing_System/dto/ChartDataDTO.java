package com.Billing_System.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChartDataDTO {
    private String name;
    private Integer sales;
    private Integer forecast;
}
