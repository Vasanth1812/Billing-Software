package com.Billing_System.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseDTO {
    private UUID id;
    private String name;
    private String code;
    private String addressLine1;
    private String city;
    private Integer totalRacks;
    private Double squareFootage;
    private Boolean temperatureControlled;
    private String managerId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
