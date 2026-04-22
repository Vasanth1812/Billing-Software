package com.Billing_System.repository;

import com.Billing_System.entity.StockLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface StockLedgerRepository extends JpaRepository<StockLedger, UUID> {

    /**
     * JOIN FETCH product so that fetching 200 ledger entries
     * does not fire 200 extra queries for the product reference.
     */
    @Query("SELECT sl FROM StockLedger sl JOIN FETCH sl.product " +
           "WHERE sl.product.id = :productId ORDER BY sl.transactionDate DESC")
    List<StockLedger> findByProductIdOrderByTransactionDateDesc(@Param("productId") UUID productId);

    @Query("SELECT sl FROM StockLedger sl JOIN FETCH sl.product WHERE sl.transactionType = :type")
    List<StockLedger> findByTransactionType(@Param("type") String transactionType);

    @Query("SELECT sl FROM StockLedger sl JOIN FETCH sl.product " +
           "WHERE sl.transactionDate BETWEEN :from AND :to ORDER BY sl.transactionDate ASC")
    List<StockLedger> findByDateRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT sl FROM StockLedger sl JOIN FETCH sl.product " +
           "WHERE sl.product.id = :productId AND sl.transactionDate BETWEEN :from AND :to " +
           "ORDER BY sl.transactionDate ASC")
    List<StockLedger> findByProductAndDateRange(
            @Param("productId") UUID productId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
