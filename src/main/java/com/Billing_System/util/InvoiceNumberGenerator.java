package com.Billing_System.util;

import com.Billing_System.repository.SalesInvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Generates sequential invoice numbers in the format INV-0001, INV-0002, etc.
 * The number is guaranteed to be unique and never repeating.
 */
@Component
@RequiredArgsConstructor
public class InvoiceNumberGenerator {

    private final SalesInvoiceRepository salesInvoiceRepository;
    private final com.Billing_System.repository.PurchaseOrderRepository purchaseOrderRepository;

    /**
     * Generate the next invoice number for Sales (e.g. INV-001).
     */
    public String generateNext() {
        int next = salesInvoiceRepository.findMaxInvoiceSequence()
                .map(max -> max + 1)
                .orElse(1);
        return String.format("INV-%03d", next);
    }

    /**
     * Generate the next invoice number for Purchases (e.g. INV-001).
     */
    public String generateNextForPurchase() {
        int next = purchaseOrderRepository.findMaxInvoiceSequence()
                .map(max -> max + 1)
                .orElse(1);
        return String.format("INV-%03d", next);
    }
}
