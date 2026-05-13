package com.Billing_System.repository;

import com.Billing_System.entity.VendorInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VendorInvoiceRepository extends JpaRepository<VendorInvoice, UUID> {
    List<VendorInvoice> findByVendorId(UUID vendorId);
    List<VendorInvoice> findByPurchaseOrderId(UUID poId);
    boolean existsByInvoiceNumberAndVendorId(String invoiceNumber, UUID vendorId);
}
