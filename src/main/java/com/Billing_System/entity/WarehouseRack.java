package com.Billing_System.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "warehouse_racks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseRack {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private String id; // e.g. R-1, R-2

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_category_id")
    private com.Billing_System.vendor.entity.VendorCategory category;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
