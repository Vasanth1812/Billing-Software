package com.Billing_System.vendor.repository;

import com.Billing_System.vendor.entity.Vendor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, UUID> {

    // Active vendors only (soft delete aware)
    List<Vendor> findAllByDeletedAtIsNullOrderByLegalNameAsc();

    Optional<Vendor> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByGstinAndDeletedAtIsNull(String gstin);
    boolean existsByPrimaryEmailAndDeletedAtIsNull(String primaryEmail);

    // Search by name, GSTIN, or vendor code
    @Query("SELECT v FROM Vendor v " +
           "WHERE v.deletedAt IS NULL " +
           "  AND (:search = '' OR " +
           "       LOWER(v.legalName)   LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "       LOWER(v.tradeName)   LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "       LOWER(v.gstin)       LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "       LOWER(v.vendorCode)  LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "  AND (:status = '' OR v.complianceStatus = :status) " +
           "  AND (:kycStatus = '' OR v.kycStatus = :kycStatus) " +
           "ORDER BY v.legalName ASC")
    List<Vendor> searchVendors(@Param("search") String search,
                               @Param("status") String status,
                               @Param("kycStatus") String kycStatus);

    // Vendors with documents expiring within a cutoff date (for compliance scan)
    @Query("SELECT DISTINCT d.vendor FROM VendorDocument d " +
           "WHERE d.vendor.deletedAt IS NULL " +
           "  AND d.uploadStatus = 'APPROVED' " +
           "  AND d.expiryDate IS NOT NULL " +
           "  AND d.expiryDate >= CURRENT_DATE " +
           "  AND d.expiryDate <= :cutoffDate " +
           "  AND d.vendor.complianceStatus <> 'BLOCKED' " +
           "ORDER BY d.vendor.legalName ASC")
    List<Vendor> findVendorsWithExpiringDocs(@Param("cutoffDate") LocalDate cutoffDate);

    // Vendors with already-expired approved documents (for blocking)
    @Query("SELECT DISTINCT d.vendor FROM VendorDocument d " +
           "WHERE d.vendor.deletedAt IS NULL " +
           "  AND d.uploadStatus = 'APPROVED' " +
           "  AND d.expiryDate IS NOT NULL " +
           "  AND d.expiryDate < CURRENT_DATE " +
           "  AND d.vendor.complianceStatus <> 'BLOCKED'")
    List<Vendor> findVendorsWithExpiredDocs();

    // Next vendor code sequence number
    @Query("SELECT COUNT(v) FROM Vendor v")
    long countAllVendors();

    // ─── Reports Hub Queries ──────────────────────────────────────────────────────

    /** Count active vendors (not soft-deleted) */
    @Query("SELECT COUNT(v) FROM Vendor v WHERE v.deletedAt IS NULL")
    long countActive();

    /** Count vendors by KYC status */
    @Query("SELECT COUNT(v) FROM Vendor v WHERE v.deletedAt IS NULL AND UPPER(v.kycStatus) = UPPER(:kycStatus)")
    long countByKycStatus(@Param("kycStatus") String kycStatus);

    /** Count vendors by compliance status */
    @Query("SELECT COUNT(v) FROM Vendor v WHERE v.deletedAt IS NULL AND UPPER(v.complianceStatus) = UPPER(:complianceStatus)")
    long countByComplianceStatus(@Param("complianceStatus") String complianceStatus);

    /** Count risk-flagged vendors (SUSPENDED, BLOCKED, or NON_COMPLIANT) */
    @Query("SELECT COUNT(v) FROM Vendor v WHERE v.deletedAt IS NULL " +
           "AND (UPPER(v.kycStatus) IN ('BLOCKED', 'REJECTED') " +
           "  OR UPPER(v.complianceStatus) IN ('BLOCKED', 'NON_COMPLIANT'))")
    long countRiskFlagged();

    /** Paginated vendor search for report data table */
    @Query("SELECT v FROM Vendor v " +
           "WHERE v.deletedAt IS NULL " +
           "  AND (:search = '' OR " +
           "       LOWER(v.legalName)   LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "       LOWER(v.tradeName)   LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "       LOWER(v.gstin)       LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "       LOWER(v.vendorCode)  LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "  AND (:kycStatus = '' OR UPPER(v.kycStatus) = UPPER(:kycStatus))")
    Page<Vendor> searchVendorsPaged(@Param("search") String search,
                                    @Param("kycStatus") String kycStatus,
                                    Pageable pageable);
}
