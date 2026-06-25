package com.Billing_System.repository;

import com.Billing_System.entity.VendorInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VendorInvoiceRepository extends JpaRepository<VendorInvoice, UUID> {
    List<VendorInvoice> findByVendorId(UUID vendorId);
    List<VendorInvoice> findByPurchaseOrderId(UUID poId);
    boolean existsByInvoiceNumberAndVendorId(String invoiceNumber, UUID vendorId);
    Optional<VendorInvoice> findByInvoiceNumber(String invoiceNumber);

    // ─── Reports Hub Queries ──────────────────────────────────────────────────────

    /** Total outstanding amount across all approved invoices */
    @Query("SELECT COALESCE(SUM(vi.totalAmount), 0) FROM VendorInvoice vi " +
           "WHERE UPPER(vi.submissionStatus) = 'APPROVED'")
    BigDecimal sumApprovedInvoiceAmount();

    /** Count invoices by submission status */
    @Query("SELECT COUNT(vi) FROM VendorInvoice vi WHERE UPPER(vi.submissionStatus) = UPPER(:status)")
    long countBySubmissionStatus(@Param("status") String status);

    /** Count all invoices with 3-way match pass */
    @Query("SELECT COUNT(vi) FROM VendorInvoice vi WHERE vi.threeWayMatch = true")
    long countThreeWayMatched();

    /** Invoices by vendor — for payables aging analysis */
    @Query("SELECT vi FROM VendorInvoice vi LEFT JOIN FETCH vi.vendor " +
           "WHERE UPPER(vi.submissionStatus) IN ('APPROVED', 'SUBMITTED', 'UNDER_REVIEW') " +
           "ORDER BY vi.invoiceDate ASC")
    List<VendorInvoice> findOutstandingInvoices();
}
