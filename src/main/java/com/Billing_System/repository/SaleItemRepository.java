package com.Billing_System.repository;

import com.Billing_System.entity.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SaleItemRepository extends JpaRepository<SaleItem, UUID> {

    /**
     * JOIN FETCH product on each SaleItem so all product names/SKUs
     * are loaded in one SQL query instead of N separate queries.
     */
    @Query("SELECT si FROM SaleItem si JOIN FETCH si.product WHERE si.salesInvoice.id = :salesInvoiceId")
    List<SaleItem> findBySalesInvoiceId(@Param("salesInvoiceId") UUID salesInvoiceId);

    @Query("SELECT si FROM SaleItem si JOIN FETCH si.product WHERE si.product.id = :productId")
    List<SaleItem> findByProductId(@Param("productId") UUID productId);
}
