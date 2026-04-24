package com.Billing_System.repository;

import com.Billing_System.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, UUID> {

       /**
        * JOIN FETCH supplier so listing purchase orders does NOT fire N queries for
        * supplier.
        * The @OneToMany items collection is handled by @BatchSize(25) on the entity.
        *
        * NOTE: You CANNOT do JOIN FETCH on both supplier (ManyToOne) AND items
        * (OneToMany)
        * in the same query when using pagination — it causes a Cartesian product.
        * 
        * @BatchSize on the items collection is the correct solution for OneToMany.
        */
       @Query("SELECT po FROM PurchaseOrder po JOIN FETCH po.supplier ORDER BY po.createdAt DESC")
       List<PurchaseOrder> findAllByOrderByCreatedAtDesc();

       /**
        * Single purchase with supplier AND items eagerly loaded in one query using
        * JOIN FETCH.
        * Safe for a single record (no Cartesian product issue for a single row).
        */
       @Query("SELECT po FROM PurchaseOrder po " +
                     "JOIN FETCH po.supplier " +
                     "LEFT JOIN FETCH po.items i " +
                     "LEFT JOIN FETCH i.product " +
                     "WHERE po.id = :id")
       Optional<PurchaseOrder> findByIdWithDetails(@Param("id") UUID id);

       @Query("SELECT po FROM PurchaseOrder po JOIN FETCH po.supplier WHERE po.supplier.id = :supplierId")
       List<PurchaseOrder> findBySupplierId(@Param("supplierId") UUID supplierId);

       @Query("SELECT po FROM PurchaseOrder po JOIN FETCH po.supplier " +
                     "WHERE po.invoiceDate BETWEEN :from AND :to ORDER BY po.invoiceDate DESC")
       List<PurchaseOrder> findByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

       /**
        * Used for dynamic invoice generation for purchases.
        */
       @Query("SELECT MAX(CAST(SUBSTRING(po.invoiceNumber, 5) AS int)) FROM PurchaseOrder po " +
                     "WHERE po.invoiceNumber LIKE 'INV-%'")
       Optional<Integer> findMaxInvoiceSequence();
}
