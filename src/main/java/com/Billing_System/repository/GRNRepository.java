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
    
    List<GRN> findBySupplierId(UUID supplierId);
    
    List<GRN> findByStatus(String status);
}
