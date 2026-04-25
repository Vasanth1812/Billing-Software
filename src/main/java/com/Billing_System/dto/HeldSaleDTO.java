package com.Billing_System.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HeldSaleDTO {
    private UUID id;
    private String label;
    private String itemsJson;
    private BigDecimal amount;
    private UUID userId;
    private LocalDateTime createdAt;
}
