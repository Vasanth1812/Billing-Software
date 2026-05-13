package com.Billing_System.vendor.event;

import java.util.UUID;

/**
 * Spring Application Event published whenever a vendor's KYC or compliance status changes.
 *
 * This is the monolith equivalent of a Kafka "VENDOR_STATUS_CHANGED" topic event.
 * Any Spring component can listen to this via @EventListener — no Kafka broker needed.
 *
 * When migrating to microservices:
 *   Replace ApplicationEventPublisher.publishEvent(this) → KafkaTemplate.send("vendor-events", this)
 *   Replace @EventListener → @KafkaListener
 *   Business logic stays 100% the same.
 */
public class VendorStatusChangedEvent {

    private final UUID   vendorId;
    private final String vendorCode;
    private final String legalName;
    private final String previousStatus;
    private final String newStatus;
    private final String statusType;   // "KYC" or "COMPLIANCE"
    private final String changedAt;

    public VendorStatusChangedEvent(UUID vendorId, String vendorCode, String legalName,
                                    String previousStatus, String newStatus, String statusType) {
        this.vendorId       = vendorId;
        this.vendorCode     = vendorCode;
        this.legalName      = legalName;
        this.previousStatus = previousStatus;
        this.newStatus      = newStatus;
        this.statusType     = statusType;
        this.changedAt      = java.time.LocalDateTime.now().toString();
    }

    public UUID   getVendorId()       { return vendorId; }
    public String getVendorCode()     { return vendorCode; }
    public String getLegalName()      { return legalName; }
    public String getPreviousStatus() { return previousStatus; }
    public String getNewStatus()      { return newStatus; }
    public String getStatusType()     { return statusType; }
    public String getChangedAt()      { return changedAt; }

    @Override
    public String toString() {
        return "VendorStatusChangedEvent{vendorCode=" + vendorCode
                + ", statusType=" + statusType
                + ", " + previousStatus + " → " + newStatus + "}";
    }
}
