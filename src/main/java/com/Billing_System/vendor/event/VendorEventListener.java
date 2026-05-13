package com.Billing_System.vendor.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listens for vendor status change events and reacts accordingly.
 *
 * This is the monolith equivalent of a Kafka consumer listening to "vendor-events" topic.
 *
 * Currently handles:
 *   1. VENDOR BLOCKED  → log warning (extend: cancel POs, send email)
 *   2. VENDOR ACTIVE   → log info  (extend: notify procurement team)
 *   3. NON_COMPLIANT   → log warning (extend: alert finance)
 *   4. EXPIRING_SOON   → log info  (extend: send SMS to vendor)
 *
 * To add email/SMS:
 *   @Autowired JavaMailSender mailSender;
 *   mailSender.send(...) inside the handler
 *
 * When migrating to microservices:
 *   Remove @EventListener → Add @KafkaListener(topics = "vendor-events")
 */
import com.Billing_System.service.NotificationService;
import com.Billing_System.vendor.entity.Vendor;
import com.Billing_System.vendor.repository.VendorRepository;

@Component
@Slf4j

public class VendorEventListener {

    private final NotificationService notificationService;
    private final VendorRepository vendorRepository;

    public VendorEventListener(NotificationService notificationService, VendorRepository vendorRepository) {
        this.notificationService = notificationService;
        this.vendorRepository = vendorRepository;
    }

    /**
     * @Async ensures this runs on a virtual thread (Java 21) without blocking
     * the main request thread. Fire-and-forget pattern.
     */
    @Async
    @EventListener
    public void handleVendorStatusChanged(VendorStatusChangedEvent event) {
        log.info("[VENDOR EVENT] {} | {} | {} → {}",
                event.getVendorCode(),
                event.getStatusType(),
                event.getPreviousStatus(),
                event.getNewStatus());

        // Fetch the Vendor entity to get their email address
        Vendor vendor = vendorRepository.findById(event.getVendorId()).orElse(null);
        if (vendor == null) return;

        // ── KYC Status Changes ────────────────────────────────────────────────
        if ("KYC".equals(event.getStatusType())) {
            switch (event.getNewStatus()) {
                case "ACTIVE" -> {
                    log.info("[VENDOR ACTIVATED] {} ({}) is now ACTIVE — procurement can raise POs",
                            event.getVendorCode(), event.getLegalName());
                    notificationService.sendVendorActivatedEmail(vendor);
                }
                case "BLOCKED" -> {
                    log.warn("[VENDOR BLOCKED] {} ({}) — all new PO creation is blocked",
                            event.getVendorCode(), event.getLegalName());
                    notificationService.sendComplianceBlockedEmail(vendor);
                }
                case "REJECTED" -> {
                    log.warn("[VENDOR REJECTED] {} ({}) — onboarding rejected",
                            event.getVendorCode(), event.getLegalName());
                    // Note: We'd ideally pass the rejection reason if it was in the event.
                    notificationService.sendVendorRejectedEmail(vendor, "Please log in to review the exact issue.");
                }
                default -> log.debug("[VENDOR KYC] {} → {}", event.getVendorCode(), event.getNewStatus());
            }
        }

        // ── Compliance Status Changes ─────────────────────────────────────────
        if ("COMPLIANCE".equals(event.getStatusType())) {
            switch (event.getNewStatus()) {
                case "NON_COMPLIANT" -> {
                    log.warn("[COMPLIANCE ALERT] {} ({}) documents expired — vendor blocked",
                            event.getVendorCode(), event.getLegalName());
                    notificationService.sendComplianceBlockedEmail(vendor);
                }
                case "EXPIRING_SOON" -> {
                    log.info("[COMPLIANCE WARNING] {} ({}) — documents expiring within 30 days",
                            event.getVendorCode(), event.getLegalName());
                    notificationService.sendComplianceWarningEmail(vendor);
                }
                case "COMPLIANT" -> {
                    log.info("[COMPLIANCE RESTORED] {} ({}) — all documents valid",
                            event.getVendorCode(), event.getLegalName());
                    // Optionally notify procurement that vendor is cleared
                }
                default -> log.debug("[COMPLIANCE] {} → {}", event.getVendorCode(), event.getNewStatus());
            }
        }
    }
}
