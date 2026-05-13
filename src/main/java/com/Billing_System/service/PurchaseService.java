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
    private final SupplierRepository supplierRepository;
    private final StockLedgerRepository stockLedgerRepository;
    private final SalesInvoiceRepository salesInvoiceRepository;
    private final TaxCalculator taxCalculator;
    private final com.Billing_System.util.InvoiceNumberGenerator invoiceNumberGenerator;
    private final com.Billing_System.vendor.repository.VendorRepository vendorRepository;

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
            // partyName: use supplier name if present, else vendor legal name
        .partyName(po.getSupplier() != null ? po.getSupplier().getName()
                : (po.getVendor() != null ? po.getVendor().getLegalName() : "Unknown"))
                    .invoiceNumber(po.getInvoiceNumber())
                    .invoiceDate(po.getInvoiceDate())
                    .amount(po.getGrandTotal())
                    .status(po.getStatus())
                    .createdAt(po.getCreatedAt())
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
        // ── Validate: at least one of supplierId OR vendorId must be provided ──
        boolean hasSupplier = dto.getSupplierId() != null && !dto.getSupplierId().isBlank();
        boolean hasVendor   = dto.getVendorId()   != null && !dto.getVendorId().isBlank();

        if (!hasSupplier && !hasVendor) {
            throw new IllegalArgumentException(
                "Either supplierId or vendorId must be provided to create a Purchase Order.");
        }

        // ── 1. Load Supplier (optional) ─────────────────────────────────────────────
        Supplier supplier = null;
        if (hasSupplier) {
            UUID supplierUuid;
            try { supplierUuid = UUID.fromString(dto.getSupplierId()); }
            catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid Supplier ID format: " + dto.getSupplierId());
            }
            supplier = supplierRepository.findById(supplierUuid)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Supplier not found with ID: " + dto.getSupplierId()));
        }

        // 1b. Validate vendor (if provided) — block PO if vendor is BLOCKED
        com.Billing_System.vendor.entity.Vendor vendor = null;
        if (dto.getVendorId() != null && !dto.getVendorId().isBlank()) {
            UUID vendorUuid;
            try { vendorUuid = UUID.fromString(dto.getVendorId()); }
            catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid Vendor ID format: " + dto.getVendorId());
            }
            vendor = vendorRepository.findByIdAndDeletedAtIsNull(vendorUuid)
                    .orElseThrow(() -> new IllegalArgumentException("Vendor not found: " + dto.getVendorId()));

            if ("BLOCKED".equals(vendor.getKycStatus()) || "BLOCKED".equals(vendor.getComplianceStatus())) {
                throw new IllegalArgumentException(
                    "Cannot raise PO for vendor '" + vendor.getLegalName() + "' (" + vendor.getVendorCode()
                    + ") — vendor is BLOCKED. Reason: kycStatus=" + vendor.getKycStatus()
                    + ", complianceStatus=" + vendor.getComplianceStatus());
            }
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
            if (product == null) {
                throw new IllegalArgumentException("Product not found with ID: " + itemDto.getProductId());
            }

            BigDecimal gstRate = itemDto.getGstRate() != null ? itemDto.getGstRate() : product.getGstRate();
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
                .supplier(supplier)
                .vendor(vendor)   // null if no vendorId provided — backward compatible
                .invoiceNumber(invoiceNumber)
                .invoiceDate(dto.getInvoiceDate() != null ? dto.getInvoiceDate() : LocalDate.now())
                .totalAmount(totalAmount)
                .gstAmount(totalTax)
                .grandTotal(grandTotal)
                .paymentMode(dto.getPaymentMode())
                .dueDate(dto.getDueDate())
                .status(dto.getStatus() != null ? dto.getStatus() : "pending")
                .build();

        // 5. Associate items with the order
        for (PurchaseItem item : lineItems) {
            item.setPurchaseOrder(order);
            order.getItems().add(item);
        }

        PurchaseOrder savedOrder = purchaseOrderRepository.save(order);

        // 6 & 7. Update stock and write ledger entries (products already in-memory, no
        // extra queries)
        for (PurchaseItem item : savedOrder.getItems()) {
            Product product = item.getProduct();
            BigDecimal currentStock = product.getCurrentStock() != null
                    ? product.getCurrentStock()
                    : BigDecimal.ZERO;

            BigDecimal newStock = currentStock.add(item.getQuantity());
            product.setCurrentStock(newStock);
            productRepository.save(product);

            StockLedger ledger = StockLedger.builder()
                    .product(product)
                    .transactionType("PURCHASE")
                    .referenceId(savedOrder.getId())
                    .quantityIn(item.getQuantity())
                    .quantityOut(BigDecimal.ZERO)
                    .balanceStock(newStock)
                    .transactionDate(LocalDateTime.now())
                    .build();
            stockLedgerRepository.save(ledger);
        }

        return savedOrder;
    }
}
