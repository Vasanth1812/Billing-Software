package com.Billing_System.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "stock_ledger")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * PURCHASE / SALE / ADJUST
     */
    @Column(name = "transaction_type", nullable = false, length = 20)
    private String transactionType;

    /**
     * ID of the purchase order or sales invoice
     */
    @Column(name = "reference_id")
    private UUID referenceId;

    @Builder.Default
    @Column(name = "quantity_in", precision = 10, scale = 3)
    private BigDecimal quantityIn = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "quantity_out", precision = 10, scale = 3)
    private BigDecimal quantityOut = BigDecimal.ZERO;

    @Column(name = "balance_stock", nullable = false, precision = 10, scale = 3)
    private BigDecimal balanceStock;

    @Builder.Default
    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate = LocalDateTime.now();
}
