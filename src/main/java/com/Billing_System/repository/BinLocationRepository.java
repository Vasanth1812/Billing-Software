package com.Billing_System.repository;

import com.Billing_System.entity.BinLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BinLocationRepository extends JpaRepository<BinLocation, UUID> {
    List<BinLocation> findByZone(String zone);
    List<BinLocation> findByCurrentProductId(UUID productId);
    boolean existsByBinFullCode(String binFullCode);
}
