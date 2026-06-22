package com.Billing_System.vendor.repository;

import com.Billing_System.vendor.entity.VendorDispute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VendorDisputeRepository extends JpaRepository<VendorDispute, UUID> {
    List<VendorDispute> findByVendorId(UUID vendorId);
}
