package com.Billing_System.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Warehouse Bin Location.
 * Represents a physical slot on a rack/shelf in the warehouse.
 */
@Entity
@Table(name = "bin_locations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BinLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // AMBIENT, COLD, FROZEN, PRODUCE, RECEIVING, DISPATCH
    @Column(name = "zone", nullable = false, length = 30)
    private String zone;

    // e.g. "A", "B", "C"
    @Column(name = "rack", nullable = false, length = 10)
    private String rack;

    // Shelf level: 1 (floor), 2, 3...
    @Column(name = "level_number", nullable = false)
    private Integer levelNumber;

    // e.g. "B04"
    @Column(name = "bin_code", nullable = false, length = 10)
    private String binCode;

    // Composite: ZONE-RACK-LEVEL-BIN (e.g. AMB-A-03-B04)
    @Column(name = "bin_full_code", nullable = false, unique = true, length = 30)
    private String binFullCode;

    // Maximum items this bin can hold
    @Column(name = "capacity_units", nullable = false, precision = 10, scale = 2)
    private BigDecimal capacityUnits;

    // Current items in this bin
    @Builder.Default
    @Column(name = "current_units", nullable = false, precision = 10, scale = 2)
    private BigDecimal currentUnits = BigDecimal.ZERO;

    // The product currently slotted here (Null if empty or mixed)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_product_id")
    private Product currentProduct;

    // FAST, MEDIUM, SLOW
    @Column(name = "velocity_class", length = 6)
    private String velocityClass;

    // For slotting logic (Fast movers should be near dispatch)
    @Column(name = "distance_from_dispatch_m", precision = 7, scale = 2)
    private BigDecimal distanceFromDispatchMeters;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
