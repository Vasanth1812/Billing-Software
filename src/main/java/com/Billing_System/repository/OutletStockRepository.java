package com.Billing_System.repository;

import com.Billing_System.entity.OutletStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OutletStockRepository extends JpaRepository<OutletStock, UUID> {
    List<OutletStock> findByProductId(UUID productId);
    Optional<OutletStock> findByProductIdAndOutletId(UUID productId, String outletId);
    
    // For global search
    List<OutletStock> findByProduct_NameContainingIgnoreCaseOrProduct_SkuContainingIgnoreCase(String name, String sku);
}
