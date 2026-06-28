package com.Billing_System.repository;

import com.Billing_System.entity.VendorPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VendorPaymentRepository extends JpaRepository<VendorPayment, UUID> {
    List<VendorPayment> findByVendorId(UUID vendorId);
    List<VendorPayment> findByInvoiceId(UUID invoiceId);
    
    @Query("SELECT COUNT(p) FROM VendorPayment p")
    long countAllPayments();

    /**
     * Eagerly loads invoice, vendor, and bankAccount in ONE query.
     * Eliminates N+1 in FinanceService.getAllVendorPayments().
     */
    @Query("SELECT p FROM VendorPayment p " +
           "LEFT JOIN FETCH p.invoice " +
           "LEFT JOIN FETCH p.vendor " +
           "LEFT JOIN FETCH p.bankAccount " +
           "ORDER BY p.createdAt DESC")
    List<VendorPayment> findAllWithDetails();

    // ─── Reports Hub Queries ──────────────────────────────────────────────────────

    /** Sum of all completed payments (net amount after deductions) */
    @Query("SELECT COALESCE(SUM(p.netPayment), 0) FROM VendorPayment p WHERE UPPER(p.status) = 'PROCESSED'")
    BigDecimal sumCompletedPayments();

    /** Sum of all hold amounts (shortage + ITC deductions) */
    @Query("SELECT COALESCE(SUM(p.holdAmount) + SUM(p.itcHoldAmount), 0) FROM VendorPayment p " +
           "WHERE UPPER(p.status) IN ('PENDING', 'APPROVED')")
    BigDecimal sumOnHoldAmount();

    /** Find the last payment for a specific vendor */
    @Query("SELECT p FROM VendorPayment p WHERE p.vendor.id = :vendorId ORDER BY p.createdAt DESC")
    List<VendorPayment> findLatestByVendorId(@Param("vendorId") UUID vendorId);

    /** Sum of payments by vendor (for computing outstanding per vendor) */
    @Query("SELECT COALESCE(SUM(p.netPayment), 0) FROM VendorPayment p " +
           "WHERE p.vendor.id = :vendorId AND UPPER(p.status) = 'PROCESSED'")
    BigDecimal sumCompletedPaymentsByVendor(@Param("vendorId") UUID vendorId);

    /** Sum of payments specifically for a list of invoices */
    @Query("SELECT COALESCE(SUM(p.netPayment), 0) FROM VendorPayment p " +
           "WHERE p.invoice.id IN :invoiceIds AND UPPER(p.status) = 'PROCESSED'")
    BigDecimal sumCompletedPaymentsByInvoices(@Param("invoiceIds") List<UUID> invoiceIds);

    /**
     * Batch query: sum of completed payments GROUPED BY invoice ID.
     * Returns Object[] = {invoice_id (UUID), sum (BigDecimal)}.
     * Replaces per-invoice loop queries in ReportService.
     */
    @Query("SELECT p.invoice.id, COALESCE(SUM(p.netPayment), 0) FROM VendorPayment p " +
           "WHERE p.invoice.id IN :invoiceIds AND UPPER(p.status) = 'PROCESSED' " +
           "GROUP BY p.invoice.id")
    List<Object[]> sumCompletedPaymentsGroupedByInvoice(@Param("invoiceIds") List<UUID> invoiceIds);

    /**
     * Batch query: latest payment date per vendor for a list of vendor IDs.
     * Returns Object[] = {vendor_id (UUID), latest_created_at (LocalDateTime)}.
     * Replaces per-vendor findLatestByVendorId() loop in ReportService.
     */
    @Query("SELECT p.vendor.id, MAX(p.createdAt) FROM VendorPayment p " +
           "WHERE p.vendor.id IN :vendorIds AND UPPER(p.status) = 'PROCESSED' " +
           "GROUP BY p.vendor.id")
    List<Object[]> findLatestPaymentDatesByVendorIds(@Param("vendorIds") List<UUID> vendorIds);
}
