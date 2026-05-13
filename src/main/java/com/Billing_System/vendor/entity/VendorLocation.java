package com.Billing_System.vendor.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Physical location of a vendor — factory, warehouse, office, or shipping point.
 * One vendor can have multiple locations; only one can be primary.
 */
@Entity
@Table(name = "vendor_locations",
        indexes = {
                @Index(name = "idx_vendor_loc_vendor", columnList = "vendor_id"),
                @Index(name = "idx_vendor_loc_primary", columnList = "vendor_id, is_primary")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    @ToString.Exclude
    private Vendor vendor;

    /** FACTORY | WAREHOUSE | OFFICE | SHIPPING_POINT */
    @Column(name = "location_type", nullable = false, length = 20)
    private String locationType;

    @Column(name = "address_line1", nullable = false, length = 120)
    private String addressLine1;

    @Column(name = "address_line2", length = 120)
    private String addressLine2;

    @Column(name = "city", nullable = false, length = 60)
    private String city;

    /** 2-char state code e.g. TN, MH, KA */
    @Column(name = "state_code", length = 2)
    private String stateCode;

    @Column(name = "pin_code", length = 6)
    private String pinCode;

    @Builder.Default
    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = false;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
