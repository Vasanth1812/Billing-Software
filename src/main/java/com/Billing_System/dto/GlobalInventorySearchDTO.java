package com.Billing_System.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class GlobalInventorySearchDTO {
    private UUID productId;
    private String productName;
    private String sku;
    private BigDecimal totalStock;
    private List<OutletStockDetail> outlets;

    @Data
    @Builder
    public static class OutletStockDetail {
        private String outletId;
        private String outletName;
        private BigDecimal stock;
        private String status;
    }
}
