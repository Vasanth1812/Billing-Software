package com.Billing_System.vendor.service;

import com.Billing_System.entity.PurchaseOrder;
import com.Billing_System.entity.User;
import com.Billing_System.repository.PurchaseOrderRepository;
import com.Billing_System.repository.UserRepository;
import com.Billing_System.vendor.dto.*;
import com.Billing_System.vendor.entity.*;
import com.Billing_System.vendor.repository.*;
import com.Billing_System.vendor.event.VendorStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VendorService {

    private final VendorRepository            vendorRepository;
    private final VendorLocationRepository    locationRepository;
    private final VendorBankAccountRepository bankAccountRepository;
    private final VendorDocumentRepository    documentRepository;
    private final UserRepository              userRepository;
    private final PurchaseOrderRepository     purchaseOrderRepository;
    private final VendorProductRepository     vendorProductRepository;
    private final ApplicationEventPublisher   eventPublisher;

    // ─── Vendor CRUD ────────────────────────────────────────────────────────────

    /**
     * Create a new vendor and start the onboarding workflow.
     * First stage is always CATEGORY_MANAGER_REVIEW.
     */
    @Transactional
    public VendorResponseDTO createVendor(VendorRequestDTO dto, UUID createdByUserId) {
        // Validate unique constraints
        if (dto.getGstin() != null && !dto.getGstin().isBlank()
                && vendorRepository.existsByGstinAndDeletedAtIsNull(dto.getGstin())) {
            throw new IllegalArgumentException("A vendor with GSTIN '" + dto.getGstin() + "' already exists.");
        }
        if (vendorRepository.existsByPrimaryEmailAndDeletedAtIsNull(dto.getPrimaryEmail())) {
            throw new IllegalArgumentException("A vendor with email '" + dto.getPrimaryEmail() + "' already exists.");
        }

        User createdBy = (createdByUserId != null)
                ? userRepository.findById(createdByUserId).orElse(null)
                : null;

        // Auto-generate vendor code: VND-000001, VND-000002 …
        long count = vendorRepository.countAllVendors();
        String vendorCode = String.format("VND-%06d", count + 1);

        Vendor vendor = Vendor.builder()
                .vendorCode(vendorCode)
                .legalName(dto.getLegalName().trim())
                .tradeName(dto.getTradeName() != null ? dto.getTradeName().trim() : null)
                .businessType(dto.getBusinessType().toUpperCase())
                .gstin(dto.getGstin())
                .panNumber(dto.getPanNumber() != null ? dto.getPanNumber().toUpperCase() : null)
                .gstRegistrationType(dto.getGstRegistrationType())
                .annualTurnoverRange(dto.getAnnualTurnoverRange())
                .primaryMobile(dto.getPrimaryMobile())
                .primaryEmail(dto.getPrimaryEmail().toLowerCase())
                .website(dto.getWebsite())
                .notes(dto.getNotes())
                .authRequired(dto.isAuthRequired())
                .build();

        // ── Onboarding flow decision ──
        if (dto.isAuthRequired()) {
            // Full 4-step authorization required — start at first stage
            vendor.setKycStatus("PENDING");
            vendor.setComplianceStatus("PENDING");
            vendor.setOnboardingStage("CATEGORY_MANAGER_REVIEW");
            log.info("Vendor created with auth workflow (4-step): {} ({})", vendor.getLegalName(), vendorCode);
        } else {
            // No authorization required — auto-activate immediately
            vendor.setKycStatus("ACTIVE");
            vendor.setComplianceStatus("COMPLIANT");
            vendor.setOnboardingStage(null); // no review stages needed
            log.info("Vendor created and auto-activated (no auth required): {} ({})", vendor.getLegalName(), vendorCode);
        }

        vendor.setCreatedBy(createdBy);
        vendor = vendorRepository.save(vendor);
        return toResponseDTO(vendor);
    }

    /** Update vendor master details */
    @Transactional
    public VendorResponseDTO updateVendor(UUID id, VendorRequestDTO dto) {
        Vendor vendor = getVendorEntity(id);

        // Check email uniqueness if changed
        if (!vendor.getPrimaryEmail().equalsIgnoreCase(dto.getPrimaryEmail())
                && vendorRepository.existsByPrimaryEmailAndDeletedAtIsNull(dto.getPrimaryEmail())) {
            throw new IllegalArgumentException("Email '" + dto.getPrimaryEmail() + "' is already used by another vendor.");
        }

        vendor.setLegalName(dto.getLegalName().trim());
        vendor.setTradeName(dto.getTradeName());
        vendor.setBusinessType(dto.getBusinessType().toUpperCase());
        vendor.setGstin(dto.getGstin());
        vendor.setPanNumber(dto.getPanNumber() != null ? dto.getPanNumber().toUpperCase() : null);
        vendor.setGstRegistrationType(dto.getGstRegistrationType());
        vendor.setAnnualTurnoverRange(dto.getAnnualTurnoverRange());
        vendor.setPrimaryMobile(dto.getPrimaryMobile());
        vendor.setPrimaryEmail(dto.getPrimaryEmail().toLowerCase());
        vendor.setWebsite(dto.getWebsite());
        vendor.setNotes(dto.getNotes());
        vendor.setUpdatedAt(LocalDateTime.now());

        vendorRepository.save(vendor);
        log.info("Vendor updated: {} ({})", vendor.getLegalName(), vendor.getVendorCode());
        return toResponseDTO(vendor);
    }

    /** Get one vendor with all sub-resources */
    @Transactional(readOnly = true)
    public VendorResponseDTO getVendorById(UUID id) {
        return toResponseDTO(getVendorEntity(id));
    }

    /** Get all active vendors — uses simple query when no filters, search query when filters provided */
    @Transactional(readOnly = true)
    public List<VendorResponseDTO> getAllVendors(String search, String complianceStatus, String kycStatus) {
        List<Vendor> vendors;

        boolean noFilters = (search == null || search.isBlank())
                         && (complianceStatus == null || complianceStatus.isBlank())
                         && (kycStatus == null || kycStatus.isBlank());

        if (noFilters) {
            // Simple SELECT * FROM vendors WHERE deleted_at IS NULL — no LIKE/LOWER needed
            vendors = vendorRepository.findAllByDeletedAtIsNullOrderByLegalNameAsc();
        } else {
            // Pass empty string (not null) for search so JPQL LIKE binding is always a string type
            String safeSearch = (search == null || search.isBlank()) ? "" : search.trim();
            vendors = vendorRepository.searchVendors(safeSearch, complianceStatus, kycStatus);
        }

        return vendors.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    /** Soft delete — sets deleted_at timestamp and deactivates status */
    @Transactional
    public void deleteVendor(UUID id) {
        Vendor vendor = getVendorEntity(id);
        vendor.setDeletedAt(LocalDateTime.now());
        vendor.setKycStatus("INACTIVE"); // Make it readable as requested
        vendor.setUpdatedAt(LocalDateTime.now());
        vendorRepository.save(vendor);
        log.info("Vendor soft-deleted and deactivated: {} ({})", vendor.getLegalName(), vendor.getVendorCode());
    }

    // ─── Onboarding Workflow ─────────────────────────────────────────────────────

    /**
     * Advance vendor onboarding to the next stage.
     * Stage order: CATEGORY_MANAGER_REVIEW → QUALITY_REVIEW → FINANCE_REVIEW → DIRECTOR_REVIEW → ACTIVE (null)
     */
    @Transactional
    public VendorResponseDTO approveOnboarding(UUID vendorId, String comments) {
        Vendor vendor = getVendorEntity(vendorId);

        if (vendor.getOnboardingStage() == null) {
            throw new IllegalStateException("Vendor '" + vendor.getLegalName() + "' is already fully onboarded (ACTIVE).");
        }

        String currentStage = vendor.getOnboardingStage();
        String nextStage;
        switch (currentStage) {
            case "CATEGORY_MANAGER_REVIEW":
                nextStage = "QUALITY_REVIEW";
                break;
            case "QUALITY_REVIEW":
                nextStage = "FINANCE_REVIEW";
                break;
            case "FINANCE_REVIEW":
                nextStage = "DIRECTOR_REVIEW";
                break;
            case "DIRECTOR_REVIEW":
                nextStage = null; // final approval - vendor goes ACTIVE
                break;
            default:
                throw new IllegalStateException("Unknown onboarding stage: " + currentStage);
        }

        vendor.setOnboardingStage(nextStage);
        vendor.setUpdatedAt(LocalDateTime.now());

        if (nextStage == null) {
            // Final approval — activate vendor
            vendor.setKycStatus("ACTIVE");
            vendor.setComplianceStatus("COMPLIANT");
            log.info("Vendor fully onboarded (ACTIVE): {} ({})", vendor.getLegalName(), vendor.getVendorCode());
        } else {
            vendor.setKycStatus("IN_REVIEW");
            log.info("Vendor onboarding advanced: {} → stage={}", vendor.getVendorCode(), nextStage);
        }

        if (comments != null && !comments.isBlank()) {
            String existing = vendor.getNotes() != null ? vendor.getNotes() + "\n" : "";
            vendor.setNotes(existing + "[" + currentStage + " APPROVED] " + comments);
        }

        vendorRepository.save(vendor);

        // Publish event (async — @EventListener in VendorEventListener handles it)
        eventPublisher.publishEvent(new VendorStatusChangedEvent(
                vendor.getId(), vendor.getVendorCode(), vendor.getLegalName(),
                "IN_REVIEW", vendor.getKycStatus(), "KYC"));

        return toResponseDTO(vendor);
    }

    /** Reject vendor at any onboarding stage */
    @Transactional
    public VendorResponseDTO rejectVendor(UUID vendorId, String reason) {
        Vendor vendor = getVendorEntity(vendorId);
        vendor.setKycStatus("REJECTED");
        vendor.setUpdatedAt(LocalDateTime.now());

        if (reason != null && !reason.isBlank()) {
            String existing = vendor.getNotes() != null ? vendor.getNotes() + "\n" : "";
            vendor.setNotes(existing + "[REJECTED] " + reason);
        }

        vendorRepository.save(vendor);
        log.info("Vendor rejected: {} — reason: {}", vendor.getVendorCode(), reason);

        eventPublisher.publishEvent(new VendorStatusChangedEvent(
                vendor.getId(), vendor.getVendorCode(), vendor.getLegalName(),
                "IN_REVIEW", "REJECTED", "KYC"));

        return toResponseDTO(vendor);
    }

    /** Block a vendor manually (compliance or fraud) */
    @Transactional
    public VendorResponseDTO blockVendor(UUID vendorId, String reason) {
        Vendor vendor = getVendorEntity(vendorId);
        vendor.setKycStatus("BLOCKED");
        vendor.setComplianceStatus("BLOCKED");
        vendor.setUpdatedAt(LocalDateTime.now());

        if (reason != null && !reason.isBlank()) {
            String existing = vendor.getNotes() != null ? vendor.getNotes() + "\n" : "";
            vendor.setNotes(existing + "[BLOCKED] " + reason);
        }

        vendorRepository.save(vendor);
        log.warn("Vendor BLOCKED: {} — reason: {}", vendor.getVendorCode(), reason);

        eventPublisher.publishEvent(new VendorStatusChangedEvent(
                vendor.getId(), vendor.getVendorCode(), vendor.getLegalName(),
                vendor.getKycStatus(), "BLOCKED", "KYC"));

        return toResponseDTO(vendor);
    }

    /** Unblock a previously blocked vendor */
    @Transactional
    public VendorResponseDTO unblockVendor(UUID vendorId) {
        Vendor vendor = getVendorEntity(vendorId);
        vendor.setKycStatus("ACTIVE");
        vendor.setComplianceStatus("COMPLIANT");
        vendor.setUpdatedAt(LocalDateTime.now());
        vendorRepository.save(vendor);
        log.info("Vendor unblocked: {}", vendor.getVendorCode());

        eventPublisher.publishEvent(new VendorStatusChangedEvent(
                vendor.getId(), vendor.getVendorCode(), vendor.getLegalName(),
                "BLOCKED", "ACTIVE", "KYC"));

        return toResponseDTO(vendor);
    }

    // ─── Purchase Order History (Phase 2) ─────────────────────────────────────

    /** All POs linked to this vendor */
    @Transactional(readOnly = true)
    public List<VendorPurchaseHistoryDTO> getVendorPurchaseOrders(UUID vendorId) {
        getVendorEntity(vendorId); // validates vendor exists
        return purchaseOrderRepository.findByVendorId(vendorId)
                .stream()
                .map(po -> VendorPurchaseHistoryDTO.builder()
                        .purchaseOrderId(po.getId())
                        .invoiceNumber(po.getInvoiceNumber())
                        .invoiceDate(po.getInvoiceDate())
                        .totalAmount(po.getTotalAmount())
                        .gstAmount(po.getGstAmount())
                        .grandTotal(po.getGrandTotal())
                        .paymentMode(po.getPaymentMode())
                        .dueDate(po.getDueDate())
                        .status(po.getStatus())
                        .itemCount(po.getItems().size())
                        .createdAt(po.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    /** Dashboard stats for a vendor */
    @Transactional(readOnly = true)
    public VendorStatsDTO getVendorStats(UUID vendorId) {
        Vendor vendor = getVendorEntity(vendorId);

        List<PurchaseOrder> pos = purchaseOrderRepository.findByVendorId(vendorId);

        BigDecimal totalSpend = pos.stream()
                .map(po -> po.getGrandTotal() != null ? po.getGrandTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int total     = pos.size();
        int pending   = (int) pos.stream().filter(po -> "pending".equals(po.getStatus())).count();
        int received  = (int) pos.stream().filter(po -> "received".equals(po.getStatus())).count();
        int cancelled = (int) pos.stream().filter(po -> "cancelled".equals(po.getStatus())).count();

        List<VendorDocument> docs = documentRepository.findByVendorIdOrderByCreatedAtDesc(vendorId);
        int docsTotal    = docs.size();
        int docsApproved = (int) docs.stream().filter(d -> "APPROVED".equals(d.getUploadStatus())).count();
        int docsPending  = (int) docs.stream().filter(d -> "PENDING".equals(d.getUploadStatus())).count();
        int docsExpired  = (int) docs.stream().filter(d ->
                "APPROVED".equals(d.getUploadStatus())
                && d.getExpiryDate() != null
                && d.getExpiryDate().isBefore(LocalDate.now())).count();

        BigDecimal avg = total > 0
                ? totalSpend.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // catalog product count
        long catalogCount = vendorProductRepository.countByVendorId(vendorId);

        return VendorStatsDTO.builder()
                .vendorCode(vendor.getVendorCode())
                .legalName(vendor.getLegalName())
                .totalPurchaseOrders(total)
                .totalSpend(totalSpend)
                .averageOrderValue(avg)
                .pendingOrders(pending)
                .receivedOrders(received)
                .cancelledOrders(cancelled)
                .catalogProductCount((int) catalogCount)
                .documentsTotal(docsTotal)
                .documentsApproved(docsApproved)
                .documentsPending(docsPending)
                .documentsExpired(docsExpired)
                .complianceStatus(vendor.getComplianceStatus())
                .kycStatus(vendor.getKycStatus())
                .build();
    }

    // ─── Locations ──────────────────────────────────────────────────────────────

    @Transactional
    public VendorResponseDTO.LocationDTO addLocation(UUID vendorId, VendorLocationDTO dto) {
        Vendor vendor = getVendorEntity(vendorId);

        // If marking as primary, demote existing primary
        if (dto.isPrimary()) {
            locationRepository.findByVendorIdOrderByIsPrimaryDesc(vendorId)
                    .forEach(l -> {
                        l.setIsPrimary(false);
                        locationRepository.save(l);
                    });
        }

        VendorLocation location = VendorLocation.builder()
                .vendor(vendor)
                .locationType(dto.getLocationType().toUpperCase())
                .addressLine1(dto.getAddressLine1())
                .addressLine2(dto.getAddressLine2())
                .city(dto.getCity())
                .stateCode(dto.getStateCode() != null ? dto.getStateCode().toUpperCase() : null)
                .pinCode(dto.getPinCode())
                .isPrimary(dto.isPrimary())
                .build();

        location = locationRepository.save(location);
        return toLocationDTO(location);
    }

    @Transactional
    public void deleteLocation(UUID locationId) {
        VendorLocation location = locationRepository.findById(locationId)
                .orElseThrow(() -> new IllegalArgumentException("Location not found: " + locationId));
        locationRepository.delete(location);
    }

    // ─── Bank Accounts ──────────────────────────────────────────────────────────

    @Transactional
    public VendorResponseDTO.BankAccountDTO addBankAccount(UUID vendorId, VendorBankAccountDTO dto) {
        Vendor vendor = getVendorEntity(vendorId);

        String hash = sha256(dto.getAccountNumber());
        if (bankAccountRepository.existsByAccountNumberHash(hash)) {
            throw new IllegalArgumentException(
                    "This bank account number already exists in the system. Duplicate accounts are not allowed.");
        }

        // Demote existing primary if needed
        if (dto.isPrimary()) {
            bankAccountRepository.findByVendorIdOrderByIsPrimaryDesc(vendorId)
                    .forEach(b -> {
                        b.setIsPrimary(false);
                        bankAccountRepository.save(b);
                    });
        }

        VendorBankAccount account = VendorBankAccount.builder()
                .vendor(vendor)
                .accountHolderName(dto.getAccountHolderName())
                .bankName(dto.getBankName())
                .accountNumber(dto.getAccountNumber())
                .accountNumberHash(hash)
                .ifscCode(dto.getIfscCode().toUpperCase())
                .accountType(dto.getAccountType().toUpperCase())
                .isPrimary(dto.isPrimary())
                .verificationStatus("PENDING")
                .build();

        account = bankAccountRepository.save(account);
        return toBankAccountDTO(account);
    }

    @Transactional
    public void deleteBankAccount(UUID accountId) {
        VendorBankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Bank account not found: " + accountId));
        bankAccountRepository.delete(account);
    }

    // ─── Documents ──────────────────────────────────────────────────────────────

    @Transactional
    public VendorResponseDTO.DocumentDTO addDocument(UUID vendorId, VendorDocumentDTO dto) {
        Vendor vendor = getVendorEntity(vendorId);

        LocalDate expiryDate = null;
        if (dto.getExpiryDate() != null && !dto.getExpiryDate().isBlank()) {
            expiryDate = LocalDate.parse(dto.getExpiryDate(), DateTimeFormatter.ISO_LOCAL_DATE);
        }

        VendorDocument doc = VendorDocument.builder()
                .vendor(vendor)
                .docType(dto.getDocType().toUpperCase())
                .docNumber(dto.getDocNumber().trim())
                .expiryDate(expiryDate)
                .fileReference(dto.getFileReference())
                .uploadStatus("PENDING")
                .build();

        doc = documentRepository.save(doc);
        log.info("Document added: type={} vendor={}", dto.getDocType(), vendor.getVendorCode());
        return toDocumentDTO(doc);
    }

    /** Approve a pending document */
    @Transactional
    public VendorResponseDTO.DocumentDTO approveDocument(UUID documentId, UUID approvedByUserId) {
        VendorDocument doc = getDocumentEntity(documentId);
        User approver = (approvedByUserId != null)
                ? userRepository.findById(approvedByUserId).orElse(null)
                : null;

        doc.setUploadStatus("APPROVED");
        doc.setVerifiedBy(approver);
        doc.setVerifiedAt(LocalDateTime.now());
        doc.setRejectionReason(null);
        documentRepository.save(doc);

        // Refresh vendor compliance after doc approval
        refreshVendorCompliance(doc.getVendor().getId());

        log.info("Document APPROVED: id={} vendor={}", documentId, doc.getVendor().getVendorCode());
        return toDocumentDTO(doc);
    }

    /** Reject a pending document with a reason */
    @Transactional
    public VendorResponseDTO.DocumentDTO rejectDocument(UUID documentId, String reason) {
        VendorDocument doc = getDocumentEntity(documentId);
        doc.setUploadStatus("REJECTED");
        doc.setRejectionReason(reason);
        doc.setVerifiedAt(LocalDateTime.now());
        documentRepository.save(doc);
        log.info("Document REJECTED: id={} reason={}", documentId, reason);
        return toDocumentDTO(doc);
    }

    @Transactional
    public void deleteDocument(UUID documentId) {
        VendorDocument doc = getDocumentEntity(documentId);
        documentRepository.delete(doc);
    }

    // ─── Vendor Products (Catalog) ───────────────────────────────────────────────

    /**
     * Add a product to a vendor's catalog (vendor_products table).
     * POST /api/vendors/{id}/products
     */
    @Transactional
    public VendorProductDTO addVendorProduct(UUID vendorId, VendorProductRequestDTO dto) {
        Vendor vendor = getVendorEntity(vendorId);

        // Vendor SKU must be unique per vendor
        if (vendorProductRepository.existsByVendorIdAndVendorSku(vendorId, dto.getVendorSku())) {
            throw new IllegalArgumentException(
                "Vendor SKU '" + dto.getVendorSku() + "' already exists for this vendor.");
        }

        VendorProduct product = VendorProduct.builder()
                .vendor(vendor)
                .productName(dto.getProductName().trim())
                .vendorSku(dto.getVendorSku().trim())
                .purchasePrice(dto.getPurchasePrice())
                .unitOfMeasure(dto.getUnitOfMeasure().trim().toUpperCase())
                .packSize(dto.getPackSize())
                .gstRate(dto.getGstRate())
                .hsnCode(dto.getHsnCode())
                .brand(dto.getBrand())
                .category(dto.getCategory())
                .minOrderQty(dto.getMinOrderQty())
                .description(dto.getDescription())
                .batchNumber(dto.getBatchNumber())
                .expiryDate(dto.getExpiryDate())
                .mappedProductId(dto.getMappedProductId())
                .isActive(true)
                .build();

        product = vendorProductRepository.save(product);
        log.info("Vendor product added: sku={} vendor={}", dto.getVendorSku(), vendor.getVendorCode());
        return toVendorProductDTO(product);
    }

    /** List all active products in a vendor's catalog */
    @Transactional(readOnly = true)
    public List<VendorProductDTO> getVendorProducts(UUID vendorId) {
        getVendorEntity(vendorId); // validate vendor exists
        return vendorProductRepository
                .findByVendorIdAndIsActiveTrueOrderByProductNameAsc(vendorId)
                .stream()
                .map(this::toVendorProductDTO)
                .collect(Collectors.toList());
    }

    /** Soft-delete a vendor product (sets isActive = false) */
    @Transactional
    public void deleteVendorProduct(UUID productId) {
        VendorProduct product = vendorProductRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Vendor product not found: " + productId));
        product.setActive(false);
        product.setUpdatedAt(LocalDateTime.now());
        vendorProductRepository.save(product);
        log.info("Vendor product deactivated: id={}", productId);
    }

    // ─── Internal Compliance Refresh ────────────────────────────────────────────

    /**
     * Called after document approval or by the compliance scheduler.
     * Refreshes vendor complianceStatus based on their approved documents' expiry.
     */
    @Transactional
    public void refreshVendorCompliance(UUID vendorId) {
        Vendor vendor = vendorRepository.findById(vendorId).orElse(null);
        if (vendor == null || vendor.getDeletedAt() != null) return;
        if ("BLOCKED".equals(vendor.getKycStatus())) return; // don't auto-unblock

        List<VendorDocument> approvedDocs = documentRepository
                .findByVendorIdOrderByCreatedAtDesc(vendorId)
                .stream()
                .filter(d -> "APPROVED".equals(d.getUploadStatus()) && d.getExpiryDate() != null)
                .toList();

        LocalDate today = LocalDate.now();
        boolean hasExpired     = approvedDocs.stream().anyMatch(d -> d.getExpiryDate().isBefore(today));
        boolean hasExpiringSoon = approvedDocs.stream()
                .anyMatch(d -> !d.getExpiryDate().isBefore(today)
                            && d.getExpiryDate().isBefore(today.plusDays(30)));

        String newStatus;
        if (hasExpired) {
            newStatus = "NON_COMPLIANT";
        } else if (hasExpiringSoon) {
            newStatus = "EXPIRING_SOON";
        } else if (vendor.getKycStatus().equals("ACTIVE")) {
            newStatus = "COMPLIANT";
        } else {
            return; // vendor not yet active — don't change compliance status
        }

        if (!newStatus.equals(vendor.getComplianceStatus())) {
            vendor.setComplianceStatus(newStatus);
            vendor.setUpdatedAt(LocalDateTime.now());
            vendorRepository.save(vendor);
            log.info("Vendor compliance refreshed: {} → {}", vendor.getVendorCode(), newStatus);
        }
    }

    // ─── Mapping Helpers ────────────────────────────────────────────────────────

    private VendorResponseDTO toResponseDTO(Vendor v) {
        List<VendorLocation>    locations    = locationRepository.findByVendorIdOrderByIsPrimaryDesc(v.getId());
        List<VendorBankAccount> bankAccounts = bankAccountRepository.findByVendorIdOrderByIsPrimaryDesc(v.getId());
        List<VendorDocument>    documents    = documentRepository.findByVendorIdOrderByCreatedAtDesc(v.getId());

        return VendorResponseDTO.builder()
                .id(v.getId())
                .vendorCode(v.getVendorCode())
                .legalName(v.getLegalName())
                .tradeName(v.getTradeName())
                .businessType(v.getBusinessType())
                .kycStatus(v.getKycStatus())
                .complianceStatus(v.getComplianceStatus())
                .onboardingStage(v.getOnboardingStage())
                .authRequired(v.isAuthRequired())
                .gstin(v.getGstin())
                .panNumber(v.getPanNumber())
                .gstRegistrationType(v.getGstRegistrationType())
                .annualTurnoverRange(v.getAnnualTurnoverRange())
                .primaryMobile(v.getPrimaryMobile())
                .primaryEmail(v.getPrimaryEmail())
                .website(v.getWebsite())
                .notes(v.getNotes())
                .createdByName(v.getCreatedBy() != null ? v.getCreatedBy().getName() : null)
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .locations(locations.stream().map(this::toLocationDTO).collect(Collectors.toList()))
                .bankAccounts(bankAccounts.stream().map(this::toBankAccountDTO).collect(Collectors.toList()))
                .documents(documents.stream().map(this::toDocumentDTO).collect(Collectors.toList()))
                .build();
    }

    private VendorSummaryDTO toSummaryDTO(Vendor v) {
        List<VendorDocument> docs = documentRepository.findByVendorIdOrderByCreatedAtDesc(v.getId())
                .stream().filter(d -> "APPROVED".equals(d.getUploadStatus()) && d.getExpiryDate() != null)
                .toList();

        LocalDate today = LocalDate.now();
        LocalDate nearestExpiry = docs.stream()
                .map(VendorDocument::getExpiryDate).filter(d -> d.isAfter(today))
                .min(LocalDate::compareTo).orElse(null);

        long expiringSoon = docs.stream()
                .filter(d -> d.getExpiryDate().isAfter(today) && d.getExpiryDate().isBefore(today.plusDays(30)))
                .count();
        long expired = docs.stream().filter(d -> d.getExpiryDate().isBefore(today)).count();

        return VendorSummaryDTO.builder()
                .id(v.getId())
                .vendorCode(v.getVendorCode())
                .legalName(v.getLegalName())
                .tradeName(v.getTradeName())
                .businessType(v.getBusinessType())
                .kycStatus(v.getKycStatus())
                .complianceStatus(v.getComplianceStatus())
                .onboardingStage(v.getOnboardingStage())
                .gstin(v.getGstin())
                .primaryMobile(v.getPrimaryMobile())
                .primaryEmail(v.getPrimaryEmail())
                .createdAt(v.getCreatedAt())
                .nearestExpiryDate(nearestExpiry != null ? nearestExpiry.toString() : null)
                .docsExpiringSoon((int) expiringSoon)
                .docsExpired((int) expired)
                .build();
    }

    private VendorResponseDTO.LocationDTO toLocationDTO(VendorLocation l) {
        return VendorResponseDTO.LocationDTO.builder()
                .id(l.getId())
                .locationType(l.getLocationType())
                .addressLine1(l.getAddressLine1())
                .addressLine2(l.getAddressLine2())
                .city(l.getCity())
                .stateCode(l.getStateCode())
                .pinCode(l.getPinCode())
                .isPrimary(l.getIsPrimary())
                .build();
    }

    private VendorResponseDTO.BankAccountDTO toBankAccountDTO(VendorBankAccount b) {
        // Mask account number — show only last 4 digits
        String masked = b.getAccountNumber().length() > 4
                ? "****" + b.getAccountNumber().substring(b.getAccountNumber().length() - 4)
                : "****";
        return VendorResponseDTO.BankAccountDTO.builder()
                .id(b.getId())
                .accountHolderName(b.getAccountHolderName())
                .bankName(b.getBankName())
                .accountNumberMasked(masked)
                .ifscCode(b.getIfscCode())
                .accountType(b.getAccountType())
                .isPrimary(b.getIsPrimary())
                .verificationStatus(b.getVerificationStatus())
                .build();
    }

    private VendorResponseDTO.DocumentDTO toDocumentDTO(VendorDocument d) {
        int daysToExpiry = 0;
        if (d.getExpiryDate() != null) {
            daysToExpiry = (int) ChronoUnit.DAYS.between(LocalDate.now(), d.getExpiryDate());
        }
        return VendorResponseDTO.DocumentDTO.builder()
                .id(d.getId())
                .docType(d.getDocType())
                .docNumber(d.getDocNumber())
                .expiryDate(d.getExpiryDate() != null ? d.getExpiryDate().toString() : null)
                .uploadStatus(d.getUploadStatus())
                .rejectionReason(d.getRejectionReason())
                .fileReference(d.getFileReference())
                .verifiedByName(d.getVerifiedBy() != null ? d.getVerifiedBy().getName() : null)
                .verifiedAt(d.getVerifiedAt() != null ? d.getVerifiedAt().toString() : null)
                .createdAt(d.getCreatedAt().toString())
                .daysToExpiry(daysToExpiry)
                .build();
    }

    // ─── Private Utils ──────────────────────────────────────────────────────────

    private Vendor getVendorEntity(UUID id) {
        return vendorRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found: " + id));
    }

    private VendorDocument getDocumentEntity(UUID id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + id));
    }

    private VendorProductDTO toVendorProductDTO(VendorProduct p) {
        return VendorProductDTO.builder()
                .id(p.getId())
                .vendorCode(p.getVendor().getVendorCode())
                .vendorLegalName(p.getVendor().getLegalName())
                .productName(p.getProductName())
                .vendorSku(p.getVendorSku())
                .purchasePrice(p.getPurchasePrice())
                .unitOfMeasure(p.getUnitOfMeasure())
                .packSize(p.getPackSize())
                .gstRate(p.getGstRate())
                .hsnCode(p.getHsnCode())
                .brand(p.getBrand())
                .category(p.getCategory())
                .minOrderQty(p.getMinOrderQty())
                .description(p.getDescription())
                .batchNumber(p.getBatchNumber())
                .expiryDate(p.getExpiryDate())
                .mappedProductId(p.getMappedProductId())
                .isActive(p.isActive())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    /** SHA-256 hash of a string (for bank account duplicate detection) */
    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute SHA-256 hash", e);
        }
    }
}
