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
}
