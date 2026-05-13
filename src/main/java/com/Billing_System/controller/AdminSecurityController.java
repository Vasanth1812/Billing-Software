package com.Billing_System.controller;

import com.Billing_System.cron.FraudDetectionCron;
import com.Billing_System.entity.AuditLog;
import com.Billing_System.entity.FraudAlert;
import com.Billing_System.repository.AuditLogRepository;
import com.Billing_System.repository.FraudAlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/security")
@RequiredArgsConstructor
public class AdminSecurityController {

    private final FraudDetectionCron fraudDetectionCron;
    private final FraudAlertRepository fraudAlertRepository;
    private final AuditLogRepository auditLogRepository;

    @PostMapping("/trigger-fraud-scan")
    public ResponseEntity<String> triggerFraudScanManually() {
        fraudDetectionCron.runNightlyFraudScan();
        return ResponseEntity.ok("Manual Fraud Scan executed successfully! Check the console logs and Fraud Alerts table.");
    }

    @GetMapping("/fraud-alerts")
    public ResponseEntity<List<Map<String, Object>>> getAllFraudAlerts() {
        List<Map<String, Object>> alerts = fraudAlertRepository.findAll().stream().map(alert -> {
            return Map.<String, Object>of(
                    "id", alert.getId(),
                    "alertType", alert.getAlertType(),
                    "severity", alert.getSeverity(),
                    "description", alert.getDescription(),
                    "vendorId", alert.getVendor() != null ? alert.getVendor().getId() : null,
                    "employeeId", alert.getEmployee() != null ? alert.getEmployee().getId() : null,
                    "status", alert.getStatus(),
                    "detectedAt", alert.getDetectedAt()
            );
        }).collect(Collectors.toList());
        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<List<Map<String, Object>>> getBlockchainAuditLogs() {
        List<Map<String, Object>> logs = auditLogRepository.findAll().stream().map(log -> {
            return Map.<String, Object>of(
                    "id", log.getId(),
                    "tableName", log.getTableName(),
                    "recordId", log.getRecordId(),
                    "action", log.getAction(),
                    "changedById", log.getChangedBy() != null ? log.getChangedBy().getId() : null,
                    "previousHash", log.getPreviousHash(),
                    "currentHash", log.getCurrentHash(),
                    "changedAt", log.getChangedAt()
            );
        }).collect(Collectors.toList());
        return ResponseEntity.ok(logs);
    }
}
