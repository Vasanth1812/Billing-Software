package com.Billing_System.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable Blockchain-style Audit Log.
 * Secures all enterprise actions using cryptographic SHA-256 hash chaining.
 */
@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // e.g. 'vendors', 'purchase_orders', 'vendor_payments'
    @Column(name = "table_name", nullable = false, length = 50)
    private String tableName;

    @Column(name = "record_id", nullable = false)
    private UUID recordId;

    // CREATE, UPDATE, DELETE, APPROVE, BLOCK
    @Column(name = "action", nullable = false, length = 20)
    private String action;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_payload", columnDefinition = "json")
    private String oldPayload; // We store JSON as String to avoid complex Jackson mappings in the entity

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_payload", columnDefinition = "json", nullable = false)
    private String newPayload;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_id", nullable = false)
    private User changedBy;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    // SHA-256 Hash of the PREVIOUS row in this table. 
    // This creates an unbreakable cryptographic chain.
    @Column(name = "previous_hash", nullable = false, length = 64)
    private String previousHash;

    // SHA-256 Hash of: previous_hash + action + record_id + new_payload + changed_at
    @Column(name = "current_hash", nullable = false, length = 64)
    private String currentHash;

    @CreationTimestamp
    @Column(name = "changed_at", updatable = false)
    private LocalDateTime changedAt;
}
