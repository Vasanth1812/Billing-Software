package com.Billing_System.service;

import com.Billing_System.dto.PurchaseRequestDTO;
import com.Billing_System.entity.*;
import com.Billing_System.repository.*;
import com.Billing_System.util.TaxCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PurchaseService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseItemRepository purchaseItemRepository;
    private final ProductRepository productRepository;
    private final StockLedgerRepository stockLedgerRepository;
    private final SalesInvoiceRepository salesInvoiceRepository;
    private final TaxCalculator taxCalculator;
    private final com.Billing_System.util.InvoiceNumberGenerator invoiceNumberGenerator;
    private final com.Billing_System.vendor.repository.VendorRepository vendorRepository;
    private final com.Billing_System.vendor.repository.VendorProductRepository vendorProductRepository;

    /** List all purchase orders + sales (merged), newest first */
    @Transactional(readOnly = true)
    public List<com.Billing_System.dto.TransactionOverviewDTO> getAllPurchases() {
        List<PurchaseOrder> orders = purchaseOrderRepository.findAllByOrderByCreatedAtDesc();
        // Trigger lazy loading
        orders.forEach(order -> order.getItems().size());

        List<com.Billing_System.dto.TransactionOverviewDTO> result = new java.util.ArrayList<>();

        // Add Purchases
        for (PurchaseOrder po : orders) {
            result.add(com.Billing_System.dto.TransactionOverviewDTO.builder()
                    .id(po.getId())
                    .type("PURCHASE")
                    // partyName: vendor is now the single procurement party
                    .partyName(po.getVendor() != null ? po.getVendor().getLegalName() : "Unknown Vendor")
                    .invoiceNumber(po.getInvoiceNumber())
                    .invoiceDate(po.getInvoiceDate())
                    .amount(po.getGrandTotal())
                    .status(po.getStatus())
                    .createdAt(po.getCreatedAt())
                    .itemCount(po.getItems() != null ? po.getItems().size() : 0)
                    .build());
        }

        // Add Sales (POS transactions)
        List<SalesInvoice> sales = salesInvoiceRepository.findAllWithItemsOrderByCreatedAtDesc();
        for (SalesInvoice si : sales) {
            result.add(com.Billing_System.dto.TransactionOverviewDTO.builder()
                    .id(si.getId())
                    .type("SALE")
                    .partyName(si.getCustomerName() != null && !si.getCustomerName().isEmpty() ? si.getCustomerName()
                            : "Cash Customer")
                    .invoiceNumber(si.getInvoiceNumber())
                    .invoiceDate(si.getInvoiceDate())
                    .amount(si.getGrandTotal())
                    .status(si.getStatus())
                    .createdAt(si.getCreatedAt())
                    .itemCount(si.getItems() != null ? si.getItems().size() : 0)
                    .build());
        }

        // Sort by created at descending
        result.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        return result;
    }

    /** Get single purchase order with line items by ID – uses JOIN FETCH, no N+1 */
    @Transactional(readOnly = true)
    public PurchaseOrder getPurchaseById(UUID id) {
        return purchaseOrderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found with id: " + id));
    }

    /**
     * Save a new purchase order atomically:
     * 1. Validate supplier
     * 2. Batch-load ALL products with ONE query (findAllById → WHERE id IN (...))
     * 3. Calculate tax per line item from in-memory map
     * 4. Insert purchase order header
     * 5. Insert line items
     * 6. INCREASE product current_stock by purchased quantity
     * 7. Record stock_ledger entry for each item
     *
     * FIX – No more loop queries:
     * Previously: 10 line items = 10 individual SELECT queries for products.
     * Now: 1 SELECT ... WHERE id IN (...) loads all products at once.
     */
    public PurchaseOrder savePurchase(PurchaseRequestDTO dto) {
        // ── 1. Validate and load Vendor (required) ──────────────────────────────────
        UUID vendorUuid;
        try { vendorUuid = UUID.fromString(dto.getVendorId()); }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Vendor ID format: " + dto.getVendorId());
        }

        com.Billing_System.vendor.entity.Vendor vendor = vendorRepository
                .findByIdAndDeletedAtIsNull(vendorUuid)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found: " + dto.getVendorId()));

        if ("BLOCKED".equals(vendor.getKycStatus()) || "BLOCKED".equals(vendor.getComplianceStatus())) {
            throw new IllegalArgumentException(
                "Cannot raise PO for vendor '" + vendor.getLegalName() + "' ("
                + vendor.getVendorCode() + ") — vendor is BLOCKED. kycStatus="
                + vendor.getKycStatus() + ", complianceStatus=" + vendor.getComplianceStatus());
        }

        // 2. Parse and Batch-load ALL products ──────────────────────────────
        List<UUID> productUuids = new ArrayList<>();
        for (PurchaseRequestDTO.PurchaseItemDTO item : dto.getItems()) {
            try {
                productUuids.add(UUID.fromString(item.getProductId()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid Product ID format: " + item.getProductId());
            }
        }

        Map<UUID, Product> productMap = productRepository.findAllById(productUuids)
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        // 3. Build line items and calculate totals
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        List<PurchaseItem> lineItems = new ArrayList<>();

        for (PurchaseRequestDTO.PurchaseItemDTO itemDto : dto.getItems()) {
            UUID pId = UUID.fromString(itemDto.getProductId());
            Product product = productMap.get(pId);
            
            // Fallback: If not in main Products, try Vendor Products
            // Priority 1: Use name from DTO if provided
            String pName = itemDto.getProductName();
            BigDecimal defaultGst = (product != null) ? product.getGstRate() : BigDecimal.valueOf(18);

            if (pName == null || pName.trim().isEmpty()) {
                // Priority 2: Use name from Store Product
                pName = (product != null) ? product.getName() : "Unknown Product";

                if (product == null) {
                    // Priority 3: Use name from VendorProduct catalog
                    var vp = vendorProductRepository.findById(pId).orElse(null);
                    if (vp != null) {
                        pName = vp.getProductName();
                        defaultGst = vp.getGstRate();
                    } else {
                        // Priority 4: Final fallback
                        pName = "Loose Item: " + itemDto.getProductId();
                    }
                }
            }

            BigDecimal gstRate = itemDto.getGstRate() != null ? itemDto.getGstRate() : defaultGst;
            if (gstRate == null)
                gstRate = BigDecimal.ZERO;

            BigDecimal discountPct = itemDto.getDiscountPct() != null
                    ? itemDto.getDiscountPct()
                    : BigDecimal.ZERO;

            TaxCalculator.TaxResult tax = taxCalculator.calculate(
                    itemDto.getQuantity(), itemDto.getPurchaseRate(), gstRate, discountPct);

            totalAmount = totalAmount.add(tax.taxableAmount());
            totalTax = totalTax.add(tax.gstAmount());

            PurchaseItem item = PurchaseItem.builder()
                    .product(product)
                    .productName(pName)
                    .vendorProductId(pId) // Store the catalog/product ID for future reference
                    .quantity(itemDto.getQuantity())
                    .purchaseRate(itemDto.getPurchaseRate())
                    .discountPct(discountPct)
                    .gstRate(gstRate)
                    .gstAmount(tax.gstAmount())
                    .totalAmount(tax.lineTotal())
                    .build();

            lineItems.add(item);
        }

        BigDecimal grandTotal = totalAmount.add(totalTax);

        // 4. Insert purchase order header
        String invoiceNumber = (dto.getInvoiceNumber() == null || dto.getInvoiceNumber().trim().isEmpty())
                ? invoiceNumberGenerator.generateNextForPurchase()
                : dto.getInvoiceNumber();

        PurchaseOrder order = PurchaseOrder.builder()
                .vendor(vendor)
                .invoiceNumber(invoiceNumber)
                .invoiceDate(dto.getInvoiceDate() != null ? dto.getInvoiceDate() : LocalDate.now())
                .totalAmount(totalAmount)
                .gstAmount(totalTax)
                .grandTotal(grandTotal)
                .paymentMode(dto.getPaymentMode())
                .dueDate(dto.getDueDate())
                .status(dto.getStatus() != null ? dto.getStatus() : "pending")
                .outletId(dto.getOutletId())
                .build();

        // 5. Associate items with the order
        for (PurchaseItem item : lineItems) {
            item.setPurchaseOrder(order);
            order.getItems().add(item);
        }

        PurchaseOrder savedOrder = purchaseOrderRepository.save(order);
        return savedOrder;
    }

    public PurchaseOrder updatePurchase(UUID id, PurchaseRequestDTO dto) {
        PurchaseOrder existing = purchaseOrderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found with ID: " + id));

        // Keep a copy of existing items mapped by their ID to resolve unmodified items
        Map<UUID, PurchaseItem> existingItemsMap = new HashMap<>();
        for (PurchaseItem item : existing.getItems()) {
            existingItemsMap.put(item.getId(), item);
        }

        // 1. Remove existing items from database to prevent duplicates
        purchaseItemRepository.deleteAll(existing.getItems());
        existing.getItems().clear();

        // Load Vendor
        UUID vendorUuid = UUID.fromString(dto.getVendorId());
        com.Billing_System.vendor.entity.Vendor vendor = vendorRepository
                .findByIdAndDeletedAtIsNull(vendorUuid)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found: " + dto.getVendorId()));

        existing.setVendor(vendor);
        existing.setInvoiceDate(dto.getInvoiceDate() != null ? dto.getInvoiceDate() : LocalDate.now());
        existing.setPaymentMode(dto.getPaymentMode());
        existing.setDueDate(dto.getDueDate());
        existing.setStatus(dto.getStatus() != null ? dto.getStatus() : "pending");
        existing.setOutletId(dto.getOutletId());

        // Collect all new product IDs that are NOT existing purchase item IDs
        List<UUID> productUuids = new ArrayList<>();
        for (PurchaseRequestDTO.PurchaseItemDTO itemDto : dto.getItems()) {
            UUID pId = UUID.fromString(itemDto.getProductId());
            if (!existingItemsMap.containsKey(pId)) {
                productUuids.add(pId);
            }
        }

        Map<UUID, Product> productMap = productRepository.findAllById(productUuids)
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        for (PurchaseRequestDTO.PurchaseItemDTO itemDto : dto.getItems()) {
            UUID pId = UUID.fromString(itemDto.getProductId());
            
            Product product = null;
            String pName = itemDto.getProductName();
            UUID vendorProductId = null;
            BigDecimal defaultGst = BigDecimal.valueOf(18);

            // Check if this item is an unmodified existing item
            if (existingItemsMap.containsKey(pId)) {
                PurchaseItem originalItem = existingItemsMap.get(pId);
                product = originalItem.getProduct();
                vendorProductId = originalItem.getVendorProductId();
                // If productName in DTO is not null/empty and not "Unknown Product", use it. Otherwise, fallback to original item's productName
                if (pName == null || pName.trim().isEmpty() || "Unknown Product".equalsIgnoreCase(pName)) {
                    pName = originalItem.getProductName();
                }
                if (product != null) {
                    defaultGst = product.getGstRate();
                } else if (vendorProductId != null) {
                    var vp = vendorProductRepository.findById(vendorProductId).orElse(null);
                    if (vp != null) {
                        defaultGst = vp.getGstRate();
                    }
                }
            } else {
                // It's a new or changed product selection
                product = productMap.get(pId);
                vendorProductId = pId; // The ID sent is the new VendorProduct/Product ID
                
                if (pName == null || pName.trim().isEmpty()) {
                    pName = (product != null) ? product.getName() : "Unknown Product";
                    if (product == null) {
                        var vp = vendorProductRepository.findById(pId).orElse(null);
                        if (vp != null) {
                            pName = vp.getProductName();
                            defaultGst = vp.getGstRate();
                        } else {
                            pName = "Loose Item: " + itemDto.getProductId();
                        }
                    }
                } else {
                    if (product != null) {
                        defaultGst = product.getGstRate();
                    } else {
                        var vp = vendorProductRepository.findById(pId).orElse(null);
                        if (vp != null) {
                            defaultGst = vp.getGstRate();
                        }
                    }
                }
            }

            BigDecimal gstRate = itemDto.getGstRate() != null ? itemDto.getGstRate() : defaultGst;
            BigDecimal discountPct = itemDto.getDiscountPct() != null ? itemDto.getDiscountPct() : BigDecimal.ZERO;

            TaxCalculator.TaxResult tax = taxCalculator.calculate(
                    itemDto.getQuantity(), itemDto.getPurchaseRate(), gstRate, discountPct);

            totalAmount = totalAmount.add(tax.taxableAmount());
            totalTax = totalTax.add(tax.gstAmount());

            PurchaseItem item = PurchaseItem.builder()
                    .purchaseOrder(existing)
                    .product(product)
                    .productName(pName)
                    .vendorProductId(vendorProductId)
                    .quantity(itemDto.getQuantity())
                    .purchaseRate(itemDto.getPurchaseRate())
                    .discountPct(discountPct)
                    .gstRate(gstRate)
                    .gstAmount(tax.gstAmount())
                    .totalAmount(tax.lineTotal())
                    .build();

            existing.getItems().add(item);
        }

        existing.setTotalAmount(totalAmount);
        existing.setGstAmount(totalTax);
        existing.setGrandTotal(totalAmount.add(totalTax));

        return purchaseOrderRepository.save(existing);
    }

    public PurchaseOrder updateStatus(UUID id, String status) {
        PurchaseOrder existing = purchaseOrderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found with ID: " + id));
        existing.setStatus(status);
        return purchaseOrderRepository.save(existing);
    }

    public PurchaseOrder vendorRespondToPO(UUID id, String status, LocalDate deliveryDate) {
        PurchaseOrder existing = purchaseOrderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Purchase order not found with ID: " + id));
        
        if ("ACTIVE".equalsIgnoreCase(status) || "ACCEPTED".equalsIgnoreCase(status)) {
            existing.setStatus("ACTIVE");
            existing.setExpectedDeliveryDate(deliveryDate);
        } else if ("DECLINED_BY_VENDOR".equalsIgnoreCase(status)) {
            existing.setStatus("DECLINED_BY_VENDOR");
        } else {
            throw new IllegalArgumentException("Invalid status for vendor response: " + status);
        }
        
        return purchaseOrderRepository.save(existing);
    }

    public void deletePurchase(UUID id) {
        if (!purchaseOrderRepository.existsById(id)) {
            throw new IllegalArgumentException("Purchase order not found with ID: " + id);
        }
        purchaseOrderRepository.deleteById(id);
    }
}
