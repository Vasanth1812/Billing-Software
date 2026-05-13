package com.Billing_System.repository;

import com.Billing_System.entity.StockTransferOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StockTransferOrderRepository extends JpaRepository<StockTransferOrder, UUID> {
    List<StockTransferOrder> findBySourceBranchName(String sourceBranch);
    List<StockTransferOrder> findByDestBranchName(String destBranch);
    List<StockTransferOrder> findByStatus(String status);
}
