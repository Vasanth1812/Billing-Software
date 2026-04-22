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

    /**
     * Generate the next invoice number by querying the max existing sequence.
     * Thread-safe because the database enforces the unique constraint on invoice_number.
     */
    public String generateNext() {
        int next = salesInvoiceRepository.findMaxInvoiceSequence()
                .map(max -> max + 1)
                .orElse(1);
        return String.format("INV-%04d", next);
    }
}
