package com.Billing_System.repository;

import com.Billing_System.entity.ReverseAuction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReverseAuctionRepository extends JpaRepository<ReverseAuction, UUID> {
    List<ReverseAuction> findByStatus(String status);
}
