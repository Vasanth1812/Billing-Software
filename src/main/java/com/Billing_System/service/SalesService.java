package com.Billing_System.service;

import com.Billing_System.dto.SaleRequestDTO;
import com.Billing_System.dto.SalesInvoiceResponseDTO;
import com.Billing_System.entity.*;
import com.Billing_System.repository.*;
import com.Billing_System.util.InvoiceNumberGenerator;
import com.Billing_System.util.TaxCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SalesService {

    private final SalesInvoiceRepository salesInvoiceRepository;
    private final SaleItemRepository saleItemRepository;
    private final ProductRepository productRepository;
    private final StockLedgerRepository stockLedgerRepository;
    private final TaxCalculator taxCalculator;
    private final InvoiceNumberGenerator invoiceNumberGenerator;

    /** List all sales invoices, newest first */
    @Transactional(readOnly = true)
    public List<SalesInvoice> getAllSales() {
        return salesInvoiceRepository.findAllWithItemsOrderByCreatedAtDesc();
    }

    /** Get single invoice with line items – uses JOIN FETCH, no N+1 */
    @Transactional(readOnly = true)
    public SalesInvoice getSaleById(UUID id) {
        return salesInvoiceRepository.findByIdWithItems(id)
                .orElseThrow(() -> new IllegalArgumentException("Sales invoice not found with id: " + id));
    }

    /**
     * Save a sale atomically (10-step sequence from document):
     * Step 1 : Receive POST /api/sales payload
     * Step 2 : Validate stock availability for each item
     * Step 3 : Generate sequential invoice number (INV-0001...)
     * Step 4 : Insert invoice header
     * Step 5 : Insert one row per product sold
     * Step 6 : Decrease current_stock for each product
     * Step 7 : Insert stock_ledger entry for each product
     * Step 8/9: Check low stock – (alert left for future implementation)
     * Step 10: Return saved invoice to caller
     *
     * FIX – No more loop queries:
     * All product IDs are collected ONCE and loaded in a single findAllById() call.
     * This turns N individual SELECT queries into exactly 1 SELECT ... WHERE id IN
     * (...).
     */
    public SalesInvoice saveSale(SaleRequestDTO dto) {

        // ── Batch-load all products in ONE query ──────────────────────────────────
        List<UUID> productIds = dto.getItems().stream()
                .map(SaleRequestDTO.SaleItemDTO::getProductId)
                .collect(Collectors.toList());

        Map<UUID, Product> productMap = productRepository.findAllById(productIds)
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        // Step 2: Validate stock availability – reads from in-memory map, NO extra DB
        // calls
        for (SaleRequestDTO.SaleItemDTO itemDto : dto.getItems()) {
            Product product = productMap.get(itemDto.getProductId());
            if (product == null) {
                throw new IllegalArgumentException("Product not found: " + itemDto.getProductId());
            }
            if (product.getCurrentStock().compareTo(itemDto.getQuantity()) < 0) {
                throw new ResponseStatusException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "Insufficient stock for product '" + product.getName() + "'. " +
                                "Available: " + product.getCurrentStock() + ", Requested: " + itemDto.getQuantity());
            }
        }

        // Step 3: Generate invoice number
        String invoiceNumber = invoiceNumberGenerator.generateNext();

        // Calculate totals – all from in-memory productMap, zero extra queries
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalCgst = BigDecimal.ZERO;
        BigDecimal totalSgst = BigDecimal.ZERO;

        List<SaleItem> lineItems = new ArrayList<>();
        List<Product> productsToUpdate = new ArrayList<>();
        List<BigDecimal> newStocks = new ArrayList<>();

        for (SaleRequestDTO.SaleItemDTO itemDto : dto.getItems()) {
            Product product = productMap.get(itemDto.getProductId());

            BigDecimal gstRate = product.getGstRate();
            if (gstRate == null)
                gstRate = BigDecimal.ZERO;

            BigDecimal discountPct = itemDto.getDiscountPct() != null ? itemDto.getDiscountPct() : BigDecimal.ZERO;

            TaxCalculator.TaxResult tax = taxCalculator.calculate(
                    itemDto.getQuantity(), itemDto.getMrp(), gstRate, discountPct);

            subtotal = subtotal.add(tax.taxableAmount());
            totalCgst = totalCgst.add(tax.cgst());
            totalSgst = totalSgst.add(tax.sgst());

            SaleItem item = SaleItem.builder()
                    .product(product)
                    .quantity(itemDto.getQuantity())
                    .mrp(itemDto.getMrp())
                    .discountPct(discountPct)
                    .gstRate(gstRate)
                    .netAmount(tax.lineTotal())
                    .build();

            lineItems.add(item);

            BigDecimal newStock = product.getCurrentStock().subtract(itemDto.getQuantity());
            productsToUpdate.add(product);
            newStocks.add(newStock);
        }

        BigDecimal grandTotal = subtotal.add(totalCgst).add(totalSgst);

        // Step 4: Insert invoice header
        SalesInvoice invoice = SalesInvoice.builder()
                .invoiceNumber(invoiceNumber)
                .customerName(dto.getCustomerName())
                .customerPhone(dto.getCustomerPhone())
                .customerGstin(dto.getCustomerGstin())
                .invoiceDate(dto.getInvoiceDate() != null ? dto.getInvoiceDate() : LocalDate.now())
                .subtotal(subtotal)
                .cgstAmount(totalCgst)
                .sgstAmount(totalSgst)
                .grandTotal(grandTotal)
                .paymentMode(dto.getPaymentMode())
                .status(dto.getStatus() != null ? dto.getStatus() : "paid")
                .build();

        // Step 5: Insert line items
        for (SaleItem item : lineItems) {
            item.setSalesInvoice(invoice);
            invoice.getItems().add(item);
        }

        SalesInvoice savedInvoice = salesInvoiceRepository.save(invoice);

        // Steps 6 & 7: Decrease stock and write ledger entries (in-memory product refs,
        // no extra queries)
        for (int i = 0; i < productsToUpdate.size(); i++) {
            Product product = productsToUpdate.get(i);
            BigDecimal newStock = newStocks.get(i);

            product.setCurrentStock(newStock);
            productRepository.save(product);

            SaleItem item = savedInvoice.getItems().get(i);
            StockLedger ledger = StockLedger.builder()
                    .product(product)
                    .transactionType("SALE")
                    .referenceId(savedInvoice.getId())
                    .quantityIn(BigDecimal.ZERO)
                    .quantityOut(item.getQuantity())
                    .balanceStock(newStock)
                    .transactionDate(LocalDateTime.now())
                    .build();
            stockLedgerRepository.save(ledger);
        }

        return savedInvoice;
    }

    /**
     * Get printable invoice data – delegates to getSaleById (already JOIN FETCHed)
     */
    @Transactional(readOnly = true)
    public SalesInvoice getPrintableInvoice(UUID id) {
        return getSaleById(id);
    }

    // Convert entity → clean DTO
    private SalesInvoiceResponseDTO toDTO(SalesInvoice si) {
        List<SalesInvoiceResponseDTO.SaleItemResponseDTO> itemDTOs = si.getItems().stream()
                .map(item -> SalesInvoiceResponseDTO.SaleItemResponseDTO.builder()
                        .id(item.getId())
                        .productName(item.getProduct().getName())
                        .hsnCode(item.getProduct().getHsnCode())
                        .sku(item.getProduct().getSku())
                        .quantity(item.getQuantity())
                        .mrp(item.getMrp())
                        .discountPct(item.getDiscountPct())
                        .gstRate(item.getGstRate())
                        .netAmount(item.getNetAmount())
                        .build())
                .collect(Collectors.toList());

        return SalesInvoiceResponseDTO.builder()
                .id(si.getId())
                .invoiceNumber(si.getInvoiceNumber())
                .customerName(si.getCustomerName())
                .customerPhone(si.getCustomerPhone())
                .customerGstin(si.getCustomerGstin())
                .invoiceDate(si.getInvoiceDate())
                .subtotal(si.getSubtotal())
                .cgstAmount(si.getCgstAmount())
                .sgstAmount(si.getSgstAmount())
                .grandTotal(si.getGrandTotal())
                .paymentMode(si.getPaymentMode())
                .status(si.getStatus())
                .createdAt(si.getCreatedAt())
                .items(itemDTOs)
                .build();
    }

    /** Get sales in a date range – used by GST reports */
    @Transactional(readOnly = true)
    public List<SalesInvoiceResponseDTO> getSalesByDateRange(LocalDate from, LocalDate to) {
        return salesInvoiceRepository.findByDateRange(from, to)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }


}
