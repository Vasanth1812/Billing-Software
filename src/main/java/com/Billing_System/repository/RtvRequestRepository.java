package com.Billing_System.repository;

import com.Billing_System.entity.RtvRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RtvRequestRepository extends JpaRepository<RtvRequest, UUID> {
    List<RtvRequest> findByVendorId(UUID vendorId);
    List<RtvRequest> findByGrnId(UUID grnId);
    List<RtvRequest> findByStatus(String status);

    /**
     * Eagerly loads all lazy relations needed by RtvService.mapToDTO() in ONE query.
     * Eliminates N+1: grn, purchaseOrder, vendor, createdBy, shortageReport.
     * Items (OneToMany) will be loaded when accessed, but as a batch.
     */
    @org.springframework.data.jpa.repository.Query(
        "SELECT DISTINCT r FROM RtvRequest r " +
        "LEFT JOIN FETCH r.grn g " +
        "LEFT JOIN FETCH r.purchaseOrder " +
        "LEFT JOIN FETCH r.vendor " +
        "LEFT JOIN FETCH r.createdBy " +
        "LEFT JOIN FETCH r.shortageReport " +
        "ORDER BY r.createdAt DESC")
    List<RtvRequest> findAllWithDetails();

    /**
     * Single RTV with all details — for getRtvById().
     */
    @org.springframework.data.jpa.repository.Query(
        "SELECT r FROM RtvRequest r " +
        "LEFT JOIN FETCH r.grn g " +
        "LEFT JOIN FETCH r.purchaseOrder " +
        "LEFT JOIN FETCH r.vendor " +
        "LEFT JOIN FETCH r.createdBy " +
        "LEFT JOIN FETCH r.shortageReport " +
        "WHERE r.id = :id")
    java.util.Optional<RtvRequest> findByIdWithDetails(@org.springframework.data.repository.query.Param("id") UUID id);
}
