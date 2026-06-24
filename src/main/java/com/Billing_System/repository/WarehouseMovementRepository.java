package com.Billing_System.repository;

import com.Billing_System.entity.WarehouseMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WarehouseMovementRepository extends JpaRepository<WarehouseMovement, UUID> {
    List<WarehouseMovement> findAllByOrderByTimestampDesc();
}
