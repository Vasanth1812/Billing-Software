package com.Billing_System.repository;

import com.Billing_System.entity.BulkUploadRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BulkUploadRowRepository extends JpaRepository<BulkUploadRow, UUID> {
    List<BulkUploadRow> findByUploadIdOrderByRowNumberAsc(UUID uploadId);
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM BulkUploadRow r WHERE r.upload.id = :uploadId")
    void deleteByUploadId(@org.springframework.data.repository.query.Param("uploadId") UUID uploadId);
}
