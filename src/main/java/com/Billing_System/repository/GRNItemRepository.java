package com.Billing_System.repository;

import com.Billing_System.entity.GRNItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GRNItemRepository extends JpaRepository<GRNItem, UUID> {

    List<GRNItem> findByGrnId(UUID grnId);
}
