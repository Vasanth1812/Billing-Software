package com.Billing_System.repository;

import com.Billing_System.entity.BulkUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BulkUploadRepository extends JpaRepository<BulkUpload, UUID> {
    List<BulkUpload> findAllByOrderByUploadedAtDesc();
}
