package com.Billing_System.repository;

import com.Billing_System.entity.FraudAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, UUID> {
    List<FraudAlert> findByStatus(String status);
    List<FraudAlert> findByVendorId(UUID vendorId);
}
