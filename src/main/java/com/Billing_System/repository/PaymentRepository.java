package com.Billing_System.repository;

import com.Billing_System.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    /**
     * All payments for a specific invoice, newest first.
     * Used by "Invoice Detail" screen to show payment history.
     */
    @Query("SELECT p FROM Payment p WHERE p.invoice.id = :invoiceId ORDER BY p.paymentDate DESC")
    List<Payment> findByInvoiceId(@Param("invoiceId") UUID invoiceId);

    /**
     * All payments in a date range, newest first.
     * Used by the Payments list screen with date filter.
     */
    List<Payment> findByPaymentDateBetweenOrderByPaymentDateDesc(LocalDate from, LocalDate to);

    /**
     * All payments, newest first (default list).
     */
    List<Payment> findAllByOrderByPaymentDateDesc();

    /**
     * Sum of all payments for an invoice.
     * Used to recompute amountPaid on the invoice after each payment.
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.invoice.id = :invoiceId")
    BigDecimal sumAmountByInvoiceId(@Param("invoiceId") UUID invoiceId);

    /**
     * Payments filtered by method (e.g. all UPI payments today).
     */
    List<Payment> findByMethodOrderByPaymentDateDesc(String method);

    /**
     * Find payment by gateway transaction ID (Razorpay / PhonePe).
     * Used in webhook handler to check for duplicate webhook delivery.
     */
    boolean existsByGatewayTxnId(String gatewayTxnId);

    /**
     * Daily total collections — used for dashboard KPI.
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.paymentDate = :date")
    BigDecimal sumByDate(@Param("date") LocalDate date);
}
