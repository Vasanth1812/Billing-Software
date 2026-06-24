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
public class WarehouseMovementDTO {
    private UUID id;
    private UUID productId;
    private String rackId;
    private String type; // IN, OUT
    private Integer quantity;
    private LocalDateTime timestamp;
}
