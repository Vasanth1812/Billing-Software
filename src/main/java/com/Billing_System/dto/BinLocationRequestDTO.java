package com.Billing_System.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class BinLocationRequestDTO {
    @NotBlank(message = "Zone is required")
    private String zone;

    @NotBlank(message = "Rack is required")
    private String rack;

    @NotNull(message = "Level number is required")
    private Integer levelNumber;

    @NotBlank(message = "Bin code is required")
    private String binCode;

    @NotNull(message = "Capacity units are required")
    private BigDecimal capacityUnits;

    private String velocityClass;
    private BigDecimal distanceFromDispatchMeters;
}
