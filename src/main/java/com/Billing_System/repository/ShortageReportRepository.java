package com.Billing_System.repository;

import com.Billing_System.entity.ShortageReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShortageReportRepository extends JpaRepository<ShortageReport, UUID> {
    
    List<ShortageReport> findByGrnId(UUID grnId);
    
    List<ShortageReport> findByPurchaseOrderId(UUID poId);
    
    List<ShortageReport> findByVendorId(UUID vendorId);
    
    @Query("SELECT COUNT(s) FROM ShortageReport s")
    long countAllReports();
}
