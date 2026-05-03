package com.Billing_System.service;

import com.Billing_System.dto.BulkUploadDetailDTO;
import com.Billing_System.dto.BulkUploadHistoryDTO;
import com.Billing_System.dto.BulkUploadTemplateDTO;
import com.Billing_System.entity.BulkUpload;
import com.Billing_System.entity.BulkUploadRow;
import com.Billing_System.entity.BulkUploadTemplate;
import com.Billing_System.repository.BulkUploadRepository;
import com.Billing_System.repository.BulkUploadRowRepository;
import com.Billing_System.repository.BulkUploadTemplateRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BulkUploadHistoryService {

    private final BulkUploadRepository bulkUploadRepository;
    private final BulkUploadRowRepository bulkUploadRowRepository;
    private final BulkUploadTemplateRepository templateRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<BulkUploadHistoryDTO> getHistory() {
        return bulkUploadRepository.findAllByOrderByUploadedAtDesc()
                .stream()
                .map(this::toHistoryDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public BulkUploadDetailDTO getUploadDetail(UUID uploadId) {
        BulkUpload upload = bulkUploadRepository.findById(uploadId)
                .orElseThrow(() -> new IllegalArgumentException("Bulk upload not found: " + uploadId));
        List<BulkUploadRow> rows = bulkUploadRowRepository.findByUploadIdOrderByRowNumberAsc(uploadId);
        return toDetailDTO(upload, rows);
    }

    @Transactional(readOnly = true)
    public List<BulkUploadTemplateDTO> getTemplates() {
        return templateRepository.findAllByOrderByLastUsedAtDesc()
                .stream()
                .map(this::toTemplateDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public BulkUploadTemplateDTO getTemplateBySupplierId(UUID supplierId) {
        BulkUploadTemplate template = templateRepository.findFirstBySupplierIdOrderByLastUsedAtDesc(supplierId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Bulk upload template not found for supplier: " + supplierId));
        return toTemplateDTO(template);
    }

    @Transactional
    public void deleteTemplateBySupplierId(UUID supplierId) {
        // Delete all templates associated with this supplier
        templateRepository.deleteBySupplierId(supplierId);
    }

    @Transactional
    public void deleteUpload(UUID uploadId) {
        BulkUpload upload = bulkUploadRepository.findById(uploadId)
                .orElseThrow(() -> new IllegalArgumentException("Bulk upload not found: " + uploadId));

        // Delete all rows associated with this upload history
        bulkUploadRowRepository.deleteByUploadId(uploadId);

        // Nullify sourceUpload in templates that refer to this upload
        List<BulkUploadTemplate> templates = templateRepository.findBySourceUploadId(uploadId);
        for (BulkUploadTemplate template : templates) {
            template.setSourceUpload(null);
            templateRepository.save(template);
        }

        // Finally delete the main upload record
        bulkUploadRepository.delete(upload);
    }

    private BulkUploadHistoryDTO toHistoryDTO(BulkUpload upload) {
        return BulkUploadHistoryDTO.builder()
                .id(upload.getId())
                .fileName(upload.getFileName())
                .uploadedAt(upload.getUploadedAt())
                .totalRows(upload.getTotalRows())
                .successCount(upload.getSuccessCount())
                .failedCount(upload.getFailedCount())
                .skippedCount(upload.getSkippedCount())
                .duplicateBarcodeCount(upload.getDuplicateBarcodeCount())
                .status(upload.getStatus())
                .autoCreateSuppliers(upload.isAutoCreateSuppliers())
                .build();
    }

    private BulkUploadDetailDTO toDetailDTO(BulkUpload upload, List<BulkUploadRow> rows) {
        return BulkUploadDetailDTO.builder()
                .id(upload.getId())
                .fileName(upload.getFileName())
                .uploadedAt(upload.getUploadedAt())
                .totalRows(upload.getTotalRows())
                .successCount(upload.getSuccessCount())
                .failedCount(upload.getFailedCount())
                .skippedCount(upload.getSkippedCount())
                .duplicateBarcodeCount(upload.getDuplicateBarcodeCount())
                .status(upload.getStatus())
                .autoCreateSuppliers(upload.isAutoCreateSuppliers())
                .rows(rows.stream().map(this::toRowDTO).toList())
                .build();
    }

    private BulkUploadDetailDTO.RowDTO toRowDTO(BulkUploadRow row) {
        return BulkUploadDetailDTO.RowDTO.builder()
                .id(row.getId())
                .rowNumber(row.getRowNumber())
                .status(row.getStatus())
                .errorMessage(row.getErrorMessage())
                .productId(row.getProductId())
                .productName(row.getProductName())
                .skuBarcode(row.getSkuBarcode())
                .category(row.getCategory())
                .unitOfMeasure(row.getUnitOfMeasure())
                .purchaseRate(row.getPurchaseRate())
                .mrp(row.getMrp())
                .gstRate(row.getGstRate())
                .hsnCode(row.getHsnCode())
                .openingStock(row.getOpeningStock())
                .minStock(row.getMinStock())
                .description(row.getDescription())
                .brand(row.getBrand())
                .supplierName(row.getSupplierName())
                .expiry(row.getExpiry())
                .active(row.getActive())
                .build();
    }

    private BulkUploadTemplateDTO toTemplateDTO(BulkUploadTemplate template) {
        return BulkUploadTemplateDTO.builder()
                .id(template.getId())
                .supplierId(template.getSupplier() != null ? template.getSupplier().getId() : null)
                .supplierName(template.getSupplierNameSnapshot())
                .templateName(template.getTemplateName())
                .columnCount(template.getColumnCount())
                .createdAt(template.getCreatedAt())
                .lastUsedAt(template.getLastUsedAt())
                .sourceUploadId(template.getSourceUpload() != null ? template.getSourceUpload().getId() : null)
                .headers(readHeaders(template.getHeadersJson()))
                .build();
    }

    private List<String> readHeaders(String headersJson) {
        try {
            return objectMapper.readValue(headersJson, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid stored template headers");
        }
    }

}
