package com.Billing_System.repository;

import com.Billing_System.entity.RtvRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RtvRequestRepository extends JpaRepository<RtvRequest, UUID> {
    List<RtvRequest> findByVendorId(UUID vendorId);
    List<RtvRequest> findByGrnId(UUID grnId);
    List<RtvRequest> findByStatus(String status);
}
