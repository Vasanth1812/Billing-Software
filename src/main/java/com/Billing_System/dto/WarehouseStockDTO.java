package com.Billing_System.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseStockDTO {
    private UUID id;
    private UUID productId;
    private String rackId;
    private Integer quantity;
    private LocalDateTime lastUpdated;
}
