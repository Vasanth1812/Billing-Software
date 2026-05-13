package com.Billing_System.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Stock Transfer Order (STO).
 * Used to move inventory between different branches or warehouses.
 */
@Entity
@Table(name = "stock_transfer_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTransferOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "sto_number", nullable = false, unique = true, length = 20)
    private String stoNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Using Strings for branch names since we don't have a separate Store entity yet
    @Column(name = "source_branch_name", nullable = false, length = 100)
    private String sourceBranchName;

    @Column(name = "dest_branch_name", nullable = false, length = 100)
    private String destBranchName;

    @Column(name = "transfer_quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal transferQuantity;

    @Column(name = "transfer_date", nullable = false)
    private LocalDate transferDate;

    // DRAFT, APPROVED, IN_TRANSIT, DELIVERED, CLOSED, CANCELLED
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    // OWN_VEHICLE, THIRD_PARTY_LOGISTICS, MANUAL_CARRY
    @Column(name = "transfer_mode", nullable = false, length = 30)
    private String transferMode;

    // NORMAL, URGENT
    @Column(name = "priority", nullable = false, length = 10)
    private String priority;

    @Column(name = "capital_saved", precision = 12, scale = 2)
    private BigDecimal capitalSaved;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private User approvedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
