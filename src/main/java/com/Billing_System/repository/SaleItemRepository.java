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

    @Query("SELECT si FROM SaleItem si JOIN FETCH si.product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.primarySupplier WHERE si.salesInvoice.invoiceDate BETWEEN :startDate AND :endDate")
    List<SaleItem> findBySalesInvoiceInvoiceDateBetween(@Param("startDate") java.time.LocalDate startDate, @Param("endDate") java.time.LocalDate endDate);

    @Query("SELECT new com.Billing_System.dto.FastMovingDTO(p.id, p.sku, p.name, c.name, SUM(si.quantity), SUM(si.netAmount)) " +
           "FROM SaleItem si JOIN si.product p LEFT JOIN p.category c " +
           "WHERE si.salesInvoice.invoiceDate BETWEEN :startDate AND :endDate " +
           "GROUP BY p.id, p.sku, p.name, c.name " +
           "ORDER BY SUM(si.quantity) DESC")
    List<com.Billing_System.dto.FastMovingDTO> findFastMovingProducts(@Param("startDate") java.time.LocalDate startDate, @Param("endDate") java.time.LocalDate endDate);

    @Query("SELECT new com.Billing_System.dto.ProfitMarginDTO(p.id, p.sku, p.name, SUM(si.quantity), SUM(si.netAmount), SUM(si.quantity * p.purchaseRate)) " +
           "FROM SaleItem si JOIN si.product p " +
           "WHERE si.salesInvoice.invoiceDate BETWEEN :startDate AND :endDate " +
           "GROUP BY p.id, p.sku, p.name " +
           "ORDER BY SUM(si.netAmount) DESC")
    List<com.Billing_System.dto.ProfitMarginDTO> findProfitMarginAnalysis(@Param("startDate") java.time.LocalDate startDate, @Param("endDate") java.time.LocalDate endDate);

    @Query("SELECT si.gstRate, SUM(si.netAmount), SUM(si.gstAmount) " +
           "FROM SaleItem si JOIN si.salesInvoice inv " +
           "WHERE EXTRACT(MONTH FROM inv.invoiceDate) = :month AND EXTRACT(YEAR FROM inv.invoiceDate) = :year " +
           "GROUP BY si.gstRate " +
           "ORDER BY si.gstRate")
    List<Object[]> findGstSalesSummary(@Param("month") int month, @Param("year") int year);
}
