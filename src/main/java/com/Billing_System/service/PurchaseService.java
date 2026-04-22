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
    private final TaxCalculator taxCalculator;

    /** List all purchase orders, newest first */
    @Transactional(readOnly = true)
    public List<PurchaseOrder> getAllPurchases() {
        return purchaseOrderRepository.findAllByOrderByCreatedAtDesc();
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

        // 1. Validate supplier (single lookup, fine)
        Supplier supplier = supplierRepository.findById(dto.getSupplierId())
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + dto.getSupplierId()));

        // 2. Batch-load ALL products in ONE query ──────────────────────────────
        List<UUID> productIds = dto.getItems().stream()
                .map(PurchaseRequestDTO.PurchaseItemDTO::getProductId)
                .collect(Collectors.toList());

        Map<UUID, Product> productMap = productRepository.findAllById(productIds)
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        // 3. Build line items and calculate totals – all from in-memory map
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        List<PurchaseItem> lineItems = new ArrayList<>();

        for (PurchaseRequestDTO.PurchaseItemDTO itemDto : dto.getItems()) {
            Product product = productMap.get(itemDto.getProductId());
            if (product == null) {
                throw new IllegalArgumentException("Product not found: " + itemDto.getProductId());
            }

            BigDecimal gstRate = itemDto.getGstRate() != null ? itemDto.getGstRate() : product.getGstRate();
            if (gstRate == null)
                gstRate = BigDecimal.ZERO;

            TaxCalculator.TaxResult tax = taxCalculator.calculate(
                    itemDto.getQuantity(), itemDto.getPurchaseRate(), gstRate, BigDecimal.ZERO);

            totalAmount = totalAmount.add(tax.taxableAmount());
            totalTax = totalTax.add(tax.gstAmount());

            PurchaseItem item = PurchaseItem.builder()
                    .product(product)
                    .quantity(itemDto.getQuantity())
                    .purchaseRate(itemDto.getPurchaseRate())
                    .gstRate(gstRate)
                    .taxAmount(tax.gstAmount())
                    .totalAmount(tax.lineTotal())
                    .build();

            lineItems.add(item);
        }

        BigDecimal grandTotal = totalAmount.add(totalTax);

        // 4. Insert purchase order header
        PurchaseOrder order = PurchaseOrder.builder()
                .supplier(supplier)
                .invoiceNumber(dto.getInvoiceNumber())
                .invoiceDate(dto.getInvoiceDate() != null ? dto.getInvoiceDate() : LocalDate.now())
                .totalAmount(totalAmount)
                .taxAmount(totalTax)
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
            BigDecimal newStock = product.getCurrentStock().add(item.getQuantity());
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
