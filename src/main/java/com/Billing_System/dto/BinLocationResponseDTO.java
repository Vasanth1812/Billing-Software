package com.Billing_System.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class BinLocationResponseDTO {
    private UUID id;
    private String zone;
    private String rack;
    private Integer levelNumber;
    private String binCode;
    private String binFullCode;
    
    private BigDecimal capacityUnits;
    private BigDecimal currentUnits;
    
    private UUID currentProductId;
    private String currentProductName;
    
    private String velocityClass;
    private BigDecimal distanceFromDispatchMeters;
    private boolean isActive;
}
