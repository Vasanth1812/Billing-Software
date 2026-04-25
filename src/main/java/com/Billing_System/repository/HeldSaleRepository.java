package com.Billing_System.repository;

import com.Billing_System.entity.HeldSale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HeldSaleRepository extends JpaRepository<HeldSale, UUID> {
    List<HeldSale> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<HeldSale> findAllByOrderByCreatedAtDesc();
}
