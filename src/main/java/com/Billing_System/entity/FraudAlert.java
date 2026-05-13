package com.Billing_System.entity;

import com.Billing_System.vendor.entity.Vendor;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Fraud Alerts table.
 * Used by the Nightly Scanner to flag suspicious activity (like Vendor-Employee collision).
 */
@Entity
@Table(name = "fraud_alerts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // VENDOR_EMPLOYEE_COLLISION, EXCESSIVE_SHORTAGES, UNAUTHORIZED_PO
    @Column(name = "alert_type", nullable = false, length = 50)
    private String alertType;

    // e.g. "CRITICAL", "HIGH", "MEDIUM"
    @Column(name = "severity", nullable = false, length = 20)
    private String severity;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private Vendor vendor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private User employee;

    // OPEN, INVESTIGATING, RESOLVED, FALSE_POSITIVE
    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @CreationTimestamp
    @Column(name = "detected_at", updatable = false)
    private LocalDateTime detectedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolution_notes", length = 500)
    private String resolutionNotes;
}
