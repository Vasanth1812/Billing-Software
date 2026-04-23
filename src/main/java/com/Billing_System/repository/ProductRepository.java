package com.Billing_System.repository;

import com.Billing_System.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    /**
     * JOIN FETCH category so that Product + Category come in ONE query.
     * Without this, fetching 100 products fires 101 queries (1 + 100 for category).
     */
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category WHERE p.isActive = true")
    List<Product> findByIsActiveTrue();

    /**
     * Single product by ID – LEFT JOIN FETCH avoids a second round-trip for category.
     */
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category WHERE p.id = :id")
    Optional<Product> findByIdWithCategory(@Param("id") UUID id);

    Optional<Product> findById(UUID id);

    Optional<Product> findBySku(String sku);

    /**
     * Exact barcode/SKU lookup – JOIN FETCH category, returns single product.
     * Used for POS barcode scan: fast exact match, not LIKE search.
     */
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category WHERE p.sku = :sku AND p.isActive = true")
    Optional<Product> findBySkuWithCategory(@Param("sku") String sku);

    boolean existsBySku(String sku);

    /**
     * Batch-load products by IDs with category JOIN FETCHed in the same query.
     * Use this instead of findAllById() anywhere products are returned in a response,
     * otherwise Product.category stays as an unloaded LAZY proxy and causes
     * "no session" error when Jackson serializes after @Transactional ends.
     */
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category WHERE p.id IN :ids")
    List<Product> findAllByIdWithCategory(@Param("ids") List<UUID> ids);

    /**
     * Search by name or SKU – JOIN FETCH category in the same query.
     */
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category WHERE p.isActive = true AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Product> searchByNameOrSku(@Param("query") String query);

    /**
     * Low stock – JOIN FETCH category so Jackson doesn't trigger N queries.
     */
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category " +
           "WHERE p.isActive = true AND p.currentStock <= p.minStock")
    List<Product> findLowStockProducts();

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.currentStock <= :threshold")
    List<Product> findProductsBelowStock(@Param("threshold") BigDecimal threshold);
}
