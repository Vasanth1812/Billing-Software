package com.Billing_System.repository;

import com.Billing_System.entity.VendorPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VendorPaymentRepository extends JpaRepository<VendorPayment, UUID> {
    List<VendorPayment> findByVendorId(UUID vendorId);
    List<VendorPayment> findByInvoiceId(UUID invoiceId);
    
    @Query("SELECT COUNT(p) FROM VendorPayment p")
    long countAllPayments();
}
