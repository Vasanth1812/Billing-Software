package com.Billing_System.vendor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "goods_receipt_notes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceiptNote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String poNumber;
    private String vendorId;
    private String receivedBy;
    private String receiptDate;
    private String status; // DRAFT, RECEIVED, SHORTAGE_ALERT, REJECTED_COMPLIANCE_BREACH

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "grn")
    private List<GrnLineItem> lineItems;
}
