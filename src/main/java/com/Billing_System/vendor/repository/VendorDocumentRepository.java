package com.Billing_System.vendor.repository;

import com.Billing_System.vendor.entity.VendorDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface VendorDocumentRepository extends JpaRepository<VendorDocument, UUID> {

    List<VendorDocument> findByVendorIdOrderByCreatedAtDesc(UUID vendorId);

    // Documents expiring within N days — used by compliance scheduler
    @Query("SELECT d FROM VendorDocument d " +
           "WHERE d.uploadStatus = 'APPROVED' " +
           "  AND d.expiryDate IS NOT NULL " +
           "  AND d.expiryDate BETWEEN :today AND :cutoff " +
           "ORDER BY d.expiryDate ASC")
    List<VendorDocument> findExpiringDocuments(@Param("today") LocalDate today,
                                                @Param("cutoff") LocalDate cutoff);

    // Already expired approved documents
    @Query("SELECT d FROM VendorDocument d " +
           "WHERE d.uploadStatus = 'APPROVED' " +
           "  AND d.expiryDate IS NOT NULL " +
           "  AND d.expiryDate < :today")
    List<VendorDocument> findExpiredDocuments(@Param("today") LocalDate today);

    void deleteByVendorId(UUID vendorId);
}
