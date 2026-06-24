package com.Billing_System.vendor.repository;

import com.Billing_System.vendor.entity.GoodsReceiptNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface GoodsReceiptNoteRepository extends JpaRepository<GoodsReceiptNote, UUID> {
}
