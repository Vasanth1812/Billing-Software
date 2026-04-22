package com.Billing_System.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BulkSaleResponseDTO {
    private int totalReceived;
    private int totalSuccess;
    private int totalFailed;
    private List<SaleResultDTO> results;

    @Data
    @Builder
    public static class SaleResultDTO {
        private String localId;        // matches frontend's localStorage key
        private String status;         // "SUCCESS" or "FAILED"
        private String invoiceNumber;  // INV-0005 (if success)
        private String errorMessage;   // reason (if failed)
    }
}
