package com.Billing_System.service;

import com.Billing_System.dto.*;
import com.Billing_System.entity.*;
import com.Billing_System.repository.*;
import com.Billing_System.vendor.entity.VendorProduct;
import com.Billing_System.vendor.repository.VendorProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GRNService {

    private final GRNRepository grnRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseItemRepository purchaseItemRepository;
    private final ProductRepository productRepository;
    private final StockLedgerRepository stockLedgerRepository;
    private final VendorProductRepository vendorProductRepository;
    private final UserRepository userRepository;
    private final ShortageReportRepository shortageReportRepository;
    private final RtvRequestRepository rtvRequestRepository;

    @Transactional
    public GRNResponseDTO createGRN(GRNRequestDTO request, UUID userId) {
        PurchaseOrder po = purchaseOrderRepository.findById(request.getPurchaseOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Purchase Order not found: " + request.getPurchaseOrderId()));

        if ("received".equalsIgnoreCase(po.getStatus()) || "cancelled".equalsIgnoreCase(po.getStatus())) {
            throw new IllegalStateException("Cannot create GRN for PO with status: " + po.getStatus());
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Generate GRN Number
        String grnNumber = "GRN-" + System.currentTimeMillis(); // Simple generation

        String initialStatus = request.getStatus() != null ? request.getStatus().toUpperCase() : "PENDING";

        GRN grn = GRN.builder()
                .grnNumber(grnNumber)
                .purchaseOrder(po)
                .vendor(po.getVendor())
                .receivedDate(request.getReceivedDate() != null ? request.getReceivedDate() : LocalDateTime.now())
                .receivedBy(user)
                .status(initialStatus)
                .vendorInvoiceNumber(request.getVendorInvoiceNumber())
                .remarks(request.getRemarks())
                .build();

        for (GRNItemRequestDTO itemDto : request.getItems()) {
            PurchaseItem poItem = purchaseItemRepository.findById(itemDto.getPurchaseItemId())
                    .orElseThrow(() -> new IllegalArgumentException("Purchase Item not found: " + itemDto.getPurchaseItemId()));
            
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + itemDto.getProductId()));
            
            VendorProduct vp = null;
            if (itemDto.getVendorProductId() != null) {
                vp = vendorProductRepository.findById(itemDto.getVendorProductId()).orElse(null);
            }

            GRNItem grnItem = GRNItem.builder()
                    .grn(grn)
                    .purchaseItem(poItem)
                    .product(product)
                    .vendorProduct(vp)
                    .orderedQuantity(itemDto.getOrderedQuantity())
                    .receivedQuantity(itemDto.getReceivedQuantity())
                    .acceptedQuantity(itemDto.getAcceptedQuantity())
                    .rejectedQuantity(itemDto.getRejectedQuantity())
                    .unitPrice(itemDto.getUnitPrice())
                    .remarks(itemDto.getRemarks())
                    .build();
            
            grn.getItems().add(grnItem);
        }

        GRN savedGrn = grnRepository.save(grn);
        
        // Auto-approve to update stock immediately for finalized GRNs
        if (!"DRAFT".equalsIgnoreCase(initialStatus)) {
            return approveGRN(savedGrn.getId());
        }
        
        return mapToDTO(savedGrn);
    }

    @Transactional
    public GRNResponseDTO approveGRN(UUID grnId) {
        GRN grn = grnRepository.findById(grnId)
                .orElseThrow(() -> new IllegalArgumentException("GRN not found: " + grnId));

        if (!"DRAFT".equals(grn.getStatus()) && !"PENDING".equals(grn.getStatus())) {
            throw new IllegalStateException("Only DRAFT or PENDING GRNs can be approved. Current status: " + grn.getStatus());
        }

        // Process each item: update stock, update PO item received qty, map vendor product
        for (GRNItem item : grn.getItems()) {
            // Update Store Inventory Stock
            Product product = item.getProduct();
            BigDecimal currentStock = product.getCurrentStock() != null ? product.getCurrentStock() : BigDecimal.ZERO;
            BigDecimal acceptedQty = item.getAcceptedQuantity() != null ? item.getAcceptedQuantity() : BigDecimal.ZERO;
            
            product.setCurrentStock(currentStock.add(acceptedQty));
            productRepository.save(product);

            // Record Stock Ledger Entry
            StockLedger ledger = StockLedger.builder()
                    .product(product)
                    .transactionType("GRN")
                    .referenceId(grn.getId())
                    .quantityIn(acceptedQty)
                    .quantityOut(BigDecimal.ZERO)
                    .balanceStock(product.getCurrentStock())
                    .transactionDate(LocalDateTime.now())
                    .reason("GRN Approval: " + grn.getGrnNumber())
                    .build();
            stockLedgerRepository.save(ledger);

            // Update Purchase Item Received Qty (This relies on tracking total received across multiple GRNs)
            PurchaseItem poItem = item.getPurchaseItem();
            BigDecimal previouslyReceived = poItem.getReceivedQuantity() != null ? poItem.getReceivedQuantity() : BigDecimal.ZERO;
            poItem.setReceivedQuantity(previouslyReceived.add(acceptedQty));
            if (poItem.getProduct() == null) {
                poItem.setProduct(product);
            }
            purchaseItemRepository.save(poItem);

            // Update Vendor Inventory Stock & Map Vendor Product to Store Product
            if (item.getVendorProduct() != null) {
                VendorProduct vp = item.getVendorProduct();
                BigDecimal currentVpStock = vp.getCurrentStock() != null ? vp.getCurrentStock() : BigDecimal.ZERO;
                vp.setCurrentStock(currentVpStock.add(acceptedQty));
                vp.setMappedProductId(product.getId());
                vp.setUpdatedAt(LocalDateTime.now());
                vendorProductRepository.save(vp);
            }
        }

        grn.setStatus("APPROVED");
        grn.setUpdatedAt(LocalDateTime.now());
        grnRepository.save(grn);

        // Update PO Status
        updatePurchaseOrderStatus(grn.getPurchaseOrder());

        // Calculate shortages and generate a Shortage Report if there are discrepancies
        BigDecimal totalShortageValue = BigDecimal.ZERO;
        
        for (GRNItem item : grn.getItems()) {
            // A shortage exists if the vendor invoiced more than we accepted (rejected goods)
            // Or if we ordered more than we accepted (short delivery - though usually we only charge back what they billed us for)
            // We'll calculate financial value of rejected items based on the unit price
            if (item.getRejectedQuantity() != null && item.getRejectedQuantity().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
                BigDecimal itemShortageValue = item.getRejectedQuantity().multiply(unitPrice);
                totalShortageValue = totalShortageValue.add(itemShortageValue);
            }
        }

        if (totalShortageValue.compareTo(BigDecimal.ZERO) > 0) {
            String reportNumber = "SR-" + System.currentTimeMillis();
            
            ShortageReport report = ShortageReport.builder()
                    .grn(grn)
                    .purchaseOrder(grn.getPurchaseOrder())
                    .vendor(grn.getVendor())
                    .reportNumber(reportNumber)
                    .totalShortageValue(totalShortageValue)
                    .status("OPEN")
                    .build();
                    
            shortageReportRepository.save(report);
            log.warn("Generated Shortage Report {} for GRN {} with value ₹{}", reportNumber, grn.getGrnNumber(), totalShortageValue);
            
            // Auto-spawn an RTV (Return To Vendor) request so the warehouse knows they need to ship physical goods back
            String rtvNumber = "RTV-" + System.currentTimeMillis();
            RtvRequest rtv = RtvRequest.builder()
                    .rtvNumber(rtvNumber)
                    .grn(grn)
                    .purchaseOrder(grn.getPurchaseOrder())
                    .vendor(grn.getVendor())
                    .status("FLAGGED")
                    .totalReturnValue(totalShortageValue)
                    .shortageReport(report)
                    .createdBy(grn.getReceivedBy()) // Usually warehouse clerk who did GRN
                    .build();
            rtvRequestRepository.save(rtv);
            log.warn("Generated RTV Request {} for damaged goods.", rtvNumber);
            
            // In a full system, you would trigger a NotificationService event here to email the vendor about the Debit Note and RTV.
        }

        return mapToDTO(grn);
    }

    private void updatePurchaseOrderStatus(PurchaseOrder po) {
        List<PurchaseItem> items = purchaseItemRepository.findByPurchaseOrderId(po.getId());
        
        boolean allFullyReceived = true;
        boolean anythingReceived = false;

        for (PurchaseItem item : items) {
            BigDecimal received = item.getReceivedQuantity() != null ? item.getReceivedQuantity() : BigDecimal.ZERO;
            BigDecimal ordered = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
            
            if (received.compareTo(BigDecimal.ZERO) > 0) anythingReceived = true;
            if (received.compareTo(ordered) < 0) allFullyReceived = false;
        }

        if (allFullyReceived) {
            po.setStatus("received");
        } else if (anythingReceived) {
            po.setStatus("partially_received");
        }
        
        purchaseOrderRepository.save(po);
    }

    @Transactional(readOnly = true)
    public List<GRNResponseDTO> getAllGRNs() {
        List<GRN> grns = grnRepository.findAllWithDetails();

        // Batch-load ALL shortage reports in ONE query instead of N queries in mapToDTO
        List<UUID> grnIds = grns.stream().map(GRN::getId).collect(Collectors.toList());
        java.util.Map<UUID, ShortageReport> shortageMap = new java.util.HashMap<>();
        if (!grnIds.isEmpty()) {
            shortageReportRepository.findByGrnIdIn(grnIds).forEach(sr ->
                shortageMap.putIfAbsent(sr.getGrn().getId(), sr));
        }

        return grns.stream()
                .map(grn -> mapToDTO(grn, shortageMap))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GRNResponseDTO> getGRNsByPO(UUID poId) {
        return grnRepository.findByPurchaseOrderId(poId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GRNResponseDTO getGRNById(UUID id) {
        GRN grn = grnRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("GRN not found: " + id));
        return mapToDTO(grn);
    }

    private GRNResponseDTO mapToDTO(GRN grn) {
        // Single-GRN lookup: query shortage report individually
        String shortageReportNumber = null;
        BigDecimal totalShortageValue = null;
        
        List<ShortageReport> reports = shortageReportRepository.findByGrnId(grn.getId());
        if (!reports.isEmpty()) {
            ShortageReport report = reports.get(0);
            shortageReportNumber = report.getReportNumber();
            totalShortageValue = report.getTotalShortageValue();
        }

        return buildGRNResponseDTO(grn, shortageReportNumber, totalShortageValue);
    }

    /**
     * Overloaded mapToDTO that uses a pre-loaded shortage map (for batch/list operations).
     * Eliminates per-GRN shortage report queries.
     */
    private GRNResponseDTO mapToDTO(GRN grn, java.util.Map<UUID, ShortageReport> shortageMap) {
        String shortageReportNumber = null;
        BigDecimal totalShortageValue = null;

        ShortageReport report = shortageMap.get(grn.getId());
        if (report != null) {
            shortageReportNumber = report.getReportNumber();
            totalShortageValue = report.getTotalShortageValue();
        }

        return buildGRNResponseDTO(grn, shortageReportNumber, totalShortageValue);
    }

    private GRNResponseDTO buildGRNResponseDTO(GRN grn, String shortageReportNumber, BigDecimal totalShortageValue) {
        return GRNResponseDTO.builder()
                .id(grn.getId())
                .grnNumber(grn.getGrnNumber())
                .purchaseOrderId(grn.getPurchaseOrder().getId())
                .purchaseOrderNumber(grn.getPurchaseOrder().getInvoiceNumber())
                .vendorId(grn.getVendor() != null ? grn.getVendor().getId() : null)
                .vendorName(grn.getVendor() != null ? grn.getVendor().getLegalName() : null)
                .receivedDate(grn.getReceivedDate())
                .receivedByUserId(grn.getReceivedBy().getId())
                .receivedByUserName(grn.getReceivedBy().getName())
                .status(grn.getStatus())
                .vendorInvoiceNumber(grn.getVendorInvoiceNumber())
                .remarks(grn.getRemarks())
                .createdAt(grn.getCreatedAt())
                .shortageReportNumber(shortageReportNumber)
                .totalShortageValue(totalShortageValue)
                .items(grn.getItems().stream().map(this::mapItemToDTO).collect(Collectors.toList()))
                .build();
    }

    private GRNItemResponseDTO mapItemToDTO(GRNItem item) {
        BigDecimal gstRate = BigDecimal.ZERO;
        if (item.getPurchaseItem() != null && item.getPurchaseItem().getGstRate() != null) {
            gstRate = item.getPurchaseItem().getGstRate();
        }
        
        return GRNItemResponseDTO.builder()
                .id(item.getId())
                .grnId(item.getGrn().getId())
                .purchaseItemId(item.getPurchaseItem().getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .productBarcode(item.getProduct().getBarcode())
                .vendorProductId(item.getVendorProduct() != null ? item.getVendorProduct().getId() : null)
                .vendorProductSku(item.getVendorProduct() != null ? item.getVendorProduct().getVendorSku() : null)
                .orderedQuantity(item.getOrderedQuantity())
                .receivedQuantity(item.getReceivedQuantity())
                .acceptedQuantity(item.getAcceptedQuantity())
                .rejectedQuantity(item.getRejectedQuantity())
                .unitPrice(item.getUnitPrice())
                .gstRate(gstRate)
                .remarks(item.getRemarks())
                .build();
    }
}
