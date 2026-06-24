package com.Billing_System.vendor.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "grn_line_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GrnLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String productId;
    private String productName;
    private Integer expectedQty;
    private Integer scannedQty;
    private Boolean isDamaged;
    private Boolean isRejected;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grn_id")
    @JsonIgnore
    private GoodsReceiptNote grn;
}
