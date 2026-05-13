package com.Billing_System.vendor.repository;

import com.Billing_System.vendor.entity.VendorLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VendorLocationRepository extends JpaRepository<VendorLocation, UUID> {
    List<VendorLocation> findByVendorIdOrderByIsPrimaryDesc(UUID vendorId);
    boolean existsByVendorIdAndIsPrimaryTrue(UUID vendorId);
    void deleteByVendorId(UUID vendorId);
}
