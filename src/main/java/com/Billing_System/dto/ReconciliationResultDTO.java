package com.Billing_System.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationResultDTO {
    private String id; // Invoice number
    private String vendor;
    private String date;
    private BigDecimal our; // System amount
    private BigDecimal theirs; // Uploaded amount
    private boolean match;
}
