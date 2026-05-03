package com.Billing_System.repository;

import com.Billing_System.entity.BulkUploadTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BulkUploadTemplateRepository extends JpaRepository<BulkUploadTemplate, UUID> {
    List<BulkUploadTemplate> findAllByOrderByLastUsedAtDesc();
    Optional<BulkUploadTemplate> findByNormalizedSupplierName(String normalizedSupplierName);

    Optional<BulkUploadTemplate> findFirstBySupplierIdOrderByLastUsedAtDesc(UUID supplierId);

    List<BulkUploadTemplate> findBySourceUploadId(UUID sourceUploadId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM BulkUploadTemplate t WHERE t.supplier.id = :supplierId")
    void deleteBySupplierId(@org.springframework.data.repository.query.Param("supplierId") UUID supplierId);
}
