package com.Billing_System.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BulkSaleRequestDTO {

    @NotEmpty(message = "Bulk request must have at least one sale")
    private List<OfflineSaleDTO> sales;

    @Data
    public static class OfflineSaleDTO {

        private String localId;        // frontend's localStorage key
        // e.g. "offline-bill-1"
        // used to tell frontend which
        // bills succeeded/failed

        @NotNull
        private SaleRequestDTO sale;   // reuse your existing DTO!
    }
}
