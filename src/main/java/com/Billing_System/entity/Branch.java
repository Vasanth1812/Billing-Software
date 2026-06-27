package com.Billing_System.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "branches")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Branch {

    @Id
    @Column(name = "id", updatable = false, nullable = false, length = 50)
    private String id;

    @Column(name = "branch_name", nullable = false, length = 100)
    private String branchName;

    @Column(name = "branch_code", length = 50)
    private String branchCode;

    @Column(name = "supermarket", length = 100)
    private String supermarket;

    @Column(name = "address_line_1")
    private String addressLine1;

    @Column(name = "address_line_2")
    private String addressLine2;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "branch_manager", length = 100)
    private String branchManager;

    @Column(name = "branch_manager_id", length = 50)
    private String branchManagerId;

    @Column(name = "contact_number", length = 20)
    private String contactNumber;

    @Column(name = "warehouse_linked", length = 100)
    private String warehouseLinked;

    @Column(name = "warehouse_id", length = 50)
    private String warehouseId;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "vendor_count")
    private Integer vendorCount;

    @Column(name = "total_stock")
    private Integer totalStock;

    @Column(name = "monthly_revenue")
    private Double monthlyRevenue;

    @Column(name = "created_on", nullable = false, updatable = false)
    private LocalDateTime createdOn;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdOn == null) createdOn = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
