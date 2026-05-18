package com.Billing_System.repository;

import com.Billing_System.entity.GSTReconciliationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Repository
public interface GSTReconciliationRepository extends JpaRepository<GSTReconciliationEntity, UUID> {
    Optional<GSTReconciliationEntity> findByGstinAndPeriod(String gstin, String period);
    List<GSTReconciliationEntity> findByPeriod(String period);
}
