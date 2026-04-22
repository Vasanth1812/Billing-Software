package com.Billing_System.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * GST/Tax calculation utility as per Indian tax rules.
 *
 * Formula from document:
 *   taxableAmount = quantity * rate * (1 - discount / 100)
 *   gstAmount     = taxableAmount * gstRate / 100
 *   cgst          = gstAmount / 2   (Central GST)
 *   sgst          = gstAmount / 2   (State GST)
 *   lineTotal     = taxableAmount + gstAmount
 */
@Component
public class TaxCalculator {

    /**
     * Calculate taxable amount (before GST) for a line item.
     */
    public BigDecimal taxableAmount(BigDecimal quantity, BigDecimal rate, BigDecimal discountPct) {
        BigDecimal discountFactor = BigDecimal.ONE.subtract(
                discountPct.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
        );
        return quantity.multiply(rate).multiply(discountFactor).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate taxable amount with zero discount.
     */
    public BigDecimal taxableAmount(BigDecimal quantity, BigDecimal rate) {
        return taxableAmount(quantity, rate, BigDecimal.ZERO);
    }

    /**
     * Calculate total GST amount on a taxable amount.
     */
    public BigDecimal gstAmount(BigDecimal taxableAmount, BigDecimal gstRate) {
        return taxableAmount.multiply(gstRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate CGST (half of total GST, 50/50 split with SGST).
     */
    public BigDecimal cgst(BigDecimal gstAmount) {
        return gstAmount.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate SGST (half of total GST, 50/50 split with CGST).
     */
    public BigDecimal sgst(BigDecimal gstAmount) {
        return gstAmount.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate line total including GST.
     */
    public BigDecimal lineTotal(BigDecimal taxableAmount, BigDecimal gstAmount) {
        return taxableAmount.add(gstAmount).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Convenient wrapper returning all computed values for a purchase/sale line item.
     */
    public TaxResult calculate(BigDecimal quantity, BigDecimal rate, BigDecimal gstRate, BigDecimal discountPct) {
        BigDecimal taxable = taxableAmount(quantity, rate, discountPct);
        BigDecimal gst     = gstAmount(taxable, gstRate);
        BigDecimal cgst    = cgst(gst);
        BigDecimal sgst    = sgst(gst);
        BigDecimal total   = lineTotal(taxable, gst);
        return new TaxResult(taxable, gst, cgst, sgst, total);
    }

    public record TaxResult(
            BigDecimal taxableAmount,
            BigDecimal gstAmount,
            BigDecimal cgst,
            BigDecimal sgst,
            BigDecimal lineTotal
    ) {}
}
