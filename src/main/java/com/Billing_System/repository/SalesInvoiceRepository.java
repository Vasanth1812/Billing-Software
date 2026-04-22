package com.Billing_System.repository;

import com.Billing_System.entity.SalesInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SalesInvoiceRepository extends JpaRepository<SalesInvoice, UUID> {

       /**
        * List all invoices – SalesInvoice has no ManyToOne to fetch here.
        * The OneToMany items collection is handled by @BatchSize(25) on the entity,
        * which batches item loading into ceil(N/25) queries instead of N queries.
        */
       @Query("SELECT DISTINCT si FROM SalesInvoice si " +
                     "LEFT JOIN FETCH si.items i " +
                     "LEFT JOIN FETCH i.product p " +
                     "LEFT JOIN FETCH p.category " +
                     "ORDER BY si.createdAt DESC")
       List<SalesInvoice> findAllWithItemsOrderByCreatedAtDesc();

       Optional<SalesInvoice> findByInvoiceNumber(String invoiceNumber);

       /**
        * Single invoice – JOIN FETCH items AND product in one query.
        * Safe for a single record (no Cartesian product / pagination issue).
        */
       @Query("SELECT s FROM SalesInvoice s " +
                     "LEFT JOIN FETCH s.items i " +
                     "LEFT JOIN FETCH i.product " +
                     "WHERE s.id = :id")
       Optional<SalesInvoice> findByIdWithItems(@Param("id") UUID id);

       @Query("SELECT DISTINCT s FROM SalesInvoice s " +
                     "LEFT JOIN FETCH s.items i " +
                     "LEFT JOIN FETCH i.product p " +
                     "LEFT JOIN FETCH p.category " +
                     "WHERE s.invoiceDate BETWEEN :from AND :to " +
                     "ORDER BY s.invoiceDate DESC")
       List<SalesInvoice> findByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

       /**
        * Used by InvoiceNumberGenerator – only reads the numeric sequence, no joins
        * needed.
        */
       @Query("SELECT MAX(CAST(SUBSTRING(s.invoiceNumber, 5) AS int)) FROM SalesInvoice s " +
                     "WHERE s.invoiceNumber LIKE 'INV-%'")
       Optional<Integer> findMaxInvoiceSequence();

       @Query("SELECT s FROM SalesInvoice s WHERE s.invoiceDate BETWEEN :from AND :to AND " +
                     "s.customerGstin IS NOT NULL AND s.customerGstin <> '' ORDER BY s.invoiceDate ASC")
       List<SalesInvoice> findB2BInvoicesInRange(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
