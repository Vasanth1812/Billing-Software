package com.Billing_System.vendor.service;

import com.Billing_System.vendor.entity.Vendor;
import com.Billing_System.vendor.entity.VendorDocument;
import com.Billing_System.vendor.repository.VendorDocumentRepository;
import com.Billing_System.vendor.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Compliance Scheduler — replaces the Kafka/CRON approach from the document
 * spec.
 *
 * Runs daily at midnight (00:00) using Java 21 virtual threads
 * (via @Scheduled).
 * Responsibilities:
 * 1. Find vendors with expired approved documents → set complianceStatus =
 * BLOCKED
 * 2. Find vendors with documents expiring within 30 days → set complianceStatus
 * = EXPIRING_SOON
 * 3. Restore COMPLIANT status for vendors whose docs are all valid again
 *
 * In production, you could emit events here via ApplicationEventPublisher
 * to send SMS/email alerts — no Kafka needed for a monolith.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ComplianceSchedulerService {

    private final VendorRepository vendorRepository;
    private final VendorDocumentRepository documentRepository;
    private final VendorService vendorService;

    /**
     * Daily compliance scan — runs every day at 00:05 AM.
     * Virtual threads handle this non-blocking even in high-load scenarios.
     *
     * Cron: "0 5 0 * * *" = At 00:05:00 every day
     */
    @Scheduled(cron = "0 5 0 * * *")
    @Transactional
    public void runDailyComplianceScan() {
        log.info("=== COMPLIANCE SCAN STARTED @ {} ===", LocalDateTime.now());

        LocalDate today = LocalDate.now();
        int blockedCount = 0, expiringSoonCount = 0, restoredCount = 0;

        // ── Step 1: Block vendors with expired documents ────────────────────────
        List<Vendor> expiredVendors = vendorRepository.findVendorsWithExpiredDocs();
        for (Vendor vendor : expiredVendors) {
            if (!"BLOCKED".equals(vendor.getComplianceStatus())) {
                vendor.setComplianceStatus("BLOCKED");
                vendor.setUpdatedAt(LocalDateTime.now());
                vendorRepository.save(vendor);
                blockedCount++;
                log.warn("COMPLIANCE BLOCK: vendor={} ({})", vendor.getVendorCode(), vendor.getLegalName());
                // TODO: send email/SMS notification here
            }
        }

        // ── Step 2: Flag vendors with documents expiring within 30 days ────────
        List<Vendor> expiringVendors = vendorRepository.findVendorsWithExpiringDocs(
                today.plusDays(30));
        // Exclude already-blocked vendors
        List<UUID> blockedIds = expiredVendors.stream().map(Vendor::getId).collect(Collectors.toList());

        for (Vendor vendor : expiringVendors) {
            if (blockedIds.contains(vendor.getId()))
                continue;
            if (!"EXPIRING_SOON".equals(vendor.getComplianceStatus())) {
                vendor.setComplianceStatus("EXPIRING_SOON");
                vendor.setUpdatedAt(LocalDateTime.now());
                vendorRepository.save(vendor);
                expiringSoonCount++;
                log.info("EXPIRING SOON: vendor={} ({})", vendor.getVendorCode(), vendor.getLegalName());
                // TODO: send reminder notification here
            }
        }

        // ── Step 3: Restore COMPLIANT status for vendors who renewed docs ───────
        // Find active vendors marked EXPIRING_SOON or NON_COMPLIANT
        // whose ALL approved docs are still valid
        List<Vendor> candidatesForRestore = vendorRepository
                .findAllByDeletedAtIsNullOrderByLegalNameAsc()
                .stream()
                .filter(v -> "ACTIVE".equals(v.getKycStatus()))
                .filter(v -> "EXPIRING_SOON".equals(v.getComplianceStatus())
                        || "NON_COMPLIANT".equals(v.getComplianceStatus()))
                .filter(v -> !blockedIds.contains(v.getId()))
                .filter(v -> expiringVendors.stream().noneMatch(ev -> ev.getId().equals(v.getId())))
                .toList();

        for (Vendor vendor : candidatesForRestore) {
            boolean hasExpired = documentRepository
                    .findExpiredDocuments(today)
                    .stream()
                    .anyMatch(d -> d.getVendor().getId().equals(vendor.getId()));

            if (!hasExpired) {
                vendor.setComplianceStatus("COMPLIANT");
                vendor.setUpdatedAt(LocalDateTime.now());
                vendorRepository.save(vendor);
                restoredCount++;
                log.info("COMPLIANCE RESTORED: vendor={} ({})", vendor.getVendorCode(), vendor.getLegalName());
            }
        }

        log.info("=== COMPLIANCE SCAN DONE — blocked={}, expiringSoon={}, restored={} ===",
                blockedCount, expiringSoonCount, restoredCount);
    }

    /**
     * Manual trigger for compliance scan — can be called from an admin API
     * endpoint.
     * Useful for testing without waiting for the daily cron.
     */
    @Transactional
    public ComplianceScanResultDTO runManualScan() {
        log.info("Manual compliance scan triggered");

        // Refresh compliance for all active vendors
        List<Vendor> allActiveVendors = vendorRepository
                .findAllByDeletedAtIsNullOrderByLegalNameAsc()
                .stream()
                .filter(v -> "ACTIVE".equals(v.getKycStatus()))
                .toList();

        for (Vendor vendor : allActiveVendors) {
            vendorService.refreshVendorCompliance(vendor.getId());
        }

        // Count statuses after refresh — use simple findAll + in-memory filter
        // (avoids PostgreSQL type inference issue with LIKE/LOWER null params)
        List<Vendor> allVendors = vendorRepository.findAllByDeletedAtIsNullOrderByLegalNameAsc();
        long compliant = allVendors.stream().filter(v -> "COMPLIANT".equals(v.getComplianceStatus())).count();
        long expiringSoon = allVendors.stream().filter(v -> "EXPIRING_SOON".equals(v.getComplianceStatus())).count();
        long nonCompliant = allVendors.stream().filter(v -> "NON_COMPLIANT".equals(v.getComplianceStatus())).count();
        long blocked = allVendors.stream().filter(v -> "BLOCKED".equals(v.getComplianceStatus())).count();

        log.info("Manual scan done — compliant={}, expiringSoon={}, nonCompliant={}, blocked={}",
                compliant, expiringSoon, nonCompliant, blocked);

        return new ComplianceScanResultDTO(compliant, expiringSoon, nonCompliant, blocked,
                LocalDateTime.now().toString());
    }

    /** Simple result holder for the manual scan response */
    public record ComplianceScanResultDTO(
            long compliant,
            long expiringSoon,
            long nonCompliant,
            long blocked,
            String scannedAt) {
    }
}
