package com.Billing_System.repository;

import com.Billing_System.entity.WarehouseStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WarehouseStockRepository extends JpaRepository<WarehouseStock, UUID> {
    List<WarehouseStock> findByRackId(String rackId);
    List<WarehouseStock> findByProductId(UUID productId);
    Optional<WarehouseStock> findByProductIdAndRackId(UUID productId, String rackId);
}
