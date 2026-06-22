package com.Billing_System.repository;

import com.Billing_System.entity.PurchaseOrder;
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
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, UUID> {

       /**
        * List all POs with vendor eagerly loaded (no N+1).
        * The @OneToMany items collection is handled by @BatchSize(25) on the entity.
        *
        * NOTE: You CANNOT do JOIN FETCH on both vendor (ManyToOne) AND items (OneToMany)
        * in the same query when using pagination — it causes a Cartesian product.
        * @BatchSize on the items collection is the correct solution for OneToMany.
        */
       @Query("SELECT po FROM PurchaseOrder po LEFT JOIN FETCH po.vendor ORDER BY po.createdAt DESC")
       List<PurchaseOrder> findAllByOrderByCreatedAtDesc();

       /**
        * Single purchase with vendor AND items eagerly loaded in one query.
        * Safe for a single record (no Cartesian product issue for a single row).
        */
       @Query("SELECT po FROM PurchaseOrder po " +
                     "LEFT JOIN FETCH po.vendor " +
                     "LEFT JOIN FETCH po.items i " +
                     "LEFT JOIN FETCH i.product " +
                     "WHERE po.id = :id")
       Optional<PurchaseOrder> findByIdWithDetails(@Param("id") UUID id);

       /** All POs linked to a specific Vendor — used for vendor PO history */
       @Query("SELECT po FROM PurchaseOrder po LEFT JOIN FETCH po.vendor WHERE po.vendor.id = :vendorId ORDER BY po.createdAt DESC")
       List<PurchaseOrder> findByVendorId(@Param("vendorId") UUID vendorId);

       /** Summary stats for a vendor — total count and spend */
       @Query("SELECT COUNT(po), COALESCE(SUM(po.grandTotal), 0) FROM PurchaseOrder po WHERE po.vendor.id = :vendorId")
       Object[] getVendorStats(@Param("vendorId") UUID vendorId);

       @Query("SELECT DISTINCT po FROM PurchaseOrder po " +
                     "LEFT JOIN FETCH po.vendor " +
                     "LEFT JOIN FETCH po.items i " +
                     "LEFT JOIN FETCH i.product " +
                     "WHERE po.invoiceDate BETWEEN :from AND :to ORDER BY po.invoiceDate DESC")
       List<PurchaseOrder> findByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

       /**
        * Used for dynamic invoice generation for purchases.
        */
       @Query("SELECT MAX(CAST(SUBSTRING(po.invoiceNumber, 5) AS int)) FROM PurchaseOrder po " +
                     "WHERE po.invoiceNumber LIKE 'INV-%'")
       Optional<Integer> findMaxInvoiceSequence();

       // ─── Reports Hub Queries ──────────────────────────────────────────────────────

       /** Count POs by status */
       @Query("SELECT COUNT(po) FROM PurchaseOrder po WHERE UPPER(po.status) = UPPER(:status)")
       long countByStatus(@Param("status") String status);

       /** Count POs with open status (PENDING, APPROVED, SENT) */
       @Query("SELECT COUNT(po) FROM PurchaseOrder po WHERE UPPER(po.status) IN ('PENDING', 'APPROVED', 'SENT')")
       long countOpenPOs();

       /** Count POs by status within a date range */
       @Query("SELECT COUNT(po) FROM PurchaseOrder po " +
              "WHERE (:status = '' OR UPPER(po.status) = UPPER(:status)) " +
              "  AND po.createdAt >= :fromDate " +
              "  AND po.createdAt <= :toDate")
       long countByStatusAndDateRange(@Param("status") String status,
                                       @Param("fromDate") java.time.LocalDateTime fromDate,
                                       @Param("toDate") java.time.LocalDateTime toDate);

       /** Count all POs within a date range */
       @Query("SELECT COUNT(po) FROM PurchaseOrder po WHERE po.createdAt >= :fromDate AND po.createdAt <= :toDate")
       long countByDateRange(@Param("fromDate") java.time.LocalDateTime fromDate,
                             @Param("toDate") java.time.LocalDateTime toDate);

       /** Paginated PO list with vendor eager-loaded, filtered by search and status */
       @Query("SELECT po FROM PurchaseOrder po LEFT JOIN FETCH po.vendor v " +
              "WHERE (:search = '' OR " +
              "       LOWER(po.invoiceNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
              "       LOWER(v.legalName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
              "       LOWER(v.vendorCode) LIKE LOWER(CONCAT('%', :search, '%'))) " +
              "  AND (:status = '' OR UPPER(po.status) = UPPER(:status))")
       Page<PurchaseOrder> searchPOsPaged(@Param("search") String search,
                                          @Param("status") String status,
                                          Pageable pageable);

       /** Count query for the paginated search (avoids FETCH JOIN in count) */
       @Query("SELECT COUNT(po) FROM PurchaseOrder po LEFT JOIN po.vendor v " +
              "WHERE (:search = '' OR " +
              "       LOWER(po.invoiceNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
              "       LOWER(v.legalName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
              "       LOWER(v.vendorCode) LIKE LOWER(CONCAT('%', :search, '%'))) " +
              "  AND (:status = '' OR UPPER(po.status) = UPPER(:status))")
       long countSearchPOs(@Param("search") String search, @Param("status") String status);
}
