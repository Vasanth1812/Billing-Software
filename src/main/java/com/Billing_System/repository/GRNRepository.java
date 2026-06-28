package com.Billing_System.repository;

import com.Billing_System.entity.GRN;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GRNRepository extends JpaRepository<GRN, UUID> {

    Optional<GRN> findByGrnNumber(String grnNumber);

    List<GRN> findByPurchaseOrderId(UUID purchaseOrderId);

    List<GRN> findByVendorId(UUID vendorId);

    List<GRN> findByStatus(String status);

    List<GRN> findByReceivedDateBetween(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate);

    /**
     * Eagerly loads vendor, purchaseOrder, and receivedBy in ONE query.
     * Items are loaded via @BatchSize on the entity.
     * Eliminates N+1 in GRNService.getAllGRNs().
     */
    @org.springframework.data.jpa.repository.Query(
        "SELECT DISTINCT g FROM GRN g " +
        "LEFT JOIN FETCH g.vendor " +
        "LEFT JOIN FETCH g.purchaseOrder " +
        "LEFT JOIN FETCH g.receivedBy " +
        "ORDER BY g.createdAt DESC")
    List<GRN> findAllWithDetails();
}
