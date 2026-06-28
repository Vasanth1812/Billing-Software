package com.Billing_System.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outlet_stocks",
        indexes = {
            @Index(name = "idx_os_product_id",  columnList = "product_id"),
            @Index(name = "idx_os_outlet_id",   columnList = "outlet_id"),
            @Index(name = "idx_os_product_outlet", columnList = "product_id, outlet_id")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutletStock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "outlet_id", nullable = false, length = 50)
    private String outletId;

    @Builder.Default
    @Column(name = "current_stock", precision = 10, scale = 3)
    private BigDecimal currentStock = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "min_stock", precision = 10, scale = 3)
    private BigDecimal minStock = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Builder.Default
    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
