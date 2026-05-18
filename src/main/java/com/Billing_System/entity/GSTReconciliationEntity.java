package com.Billing_System.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "gst_reconciliations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GSTReconciliationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "gstin", nullable = false, length = 15)
    private String gstin;

    @Column(name = "period", nullable = false, length = 7)
    private String period; // Format: YYYY-MM

    @Column(name = "dispute_note", length = 300)
    private String disputeNote;

    @Column(name = "notified", nullable = false)
    private boolean notified;

    @Column(name = "released", nullable = false)
    private boolean released;

    @Column(name = "written_off", nullable = false)
    private boolean writtenOff;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
