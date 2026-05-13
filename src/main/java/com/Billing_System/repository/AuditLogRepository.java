package com.Billing_System.repository;

import com.Billing_System.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    
    // Get the most recent log entry to extract its hash
    @Query(value = "SELECT * FROM audit_logs ORDER BY changed_at DESC LIMIT 1", nativeQuery = true)
    Optional<AuditLog> findLatestLog();
    
    List<AuditLog> findByTableNameOrderByChangedAtDesc(String tableName);
    List<AuditLog> findByRecordIdOrderByChangedAtDesc(UUID recordId);
}
