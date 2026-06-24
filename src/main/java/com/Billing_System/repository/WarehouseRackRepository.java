package com.Billing_System.repository;

import com.Billing_System.entity.WarehouseRack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarehouseRackRepository extends JpaRepository<WarehouseRack, String> {
    List<WarehouseRack> findByCategoryId(java.util.UUID categoryId);
}
