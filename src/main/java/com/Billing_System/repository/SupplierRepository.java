package com.Billing_System.repository;

import com.Billing_System.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, UUID> {

    List<Supplier> findByIsActiveTrueOrderByNameAsc();

    boolean existsByGstin(String gstin);
}
