package com.Billing_System.vendor.repository;

import com.Billing_System.vendor.entity.VendorCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VendorCategoryRepository extends JpaRepository<VendorCategory, UUID> {
    Optional<VendorCategory> findByNameIgnoreCase(String name);
}
