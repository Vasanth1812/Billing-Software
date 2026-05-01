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
       @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category")
       List<Product> findAllWithCategory();

       /**
        * Single product by ID â€“ LEFT JOIN FETCH avoids a second round-trip for
        * category.
        */
       @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category WHERE p.id = :id")
       Optional<Product> findByIdWithCategory(@Param("id") UUID id);

       Optional<Product> findById(UUID id);

       Optional<Product> findBySku(String sku);

       /**
        * Exact barcode/SKU lookup â€“ JOIN FETCH category, returns single product.
        * Used for POS barcode scan: fast exact match, not LIKE search.
        */
       @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category WHERE (p.sku = :sku OR p.barcode = :sku)")
       Optional<Product> findBySkuWithCategory(@Param("sku") String sku);

       boolean existsBySku(String sku);

       /**
        * Load ONLY the SKU strings of all products â€” used for O(1) duplicate check
        * during bulk import. Much faster than loading full Product objects.
        */
       @Query("SELECT p.sku FROM Product p")
       List<String> findAllSkus();

       /**
        * Batch-load products by IDs with category JOIN FETCHed in the same query.
        * Use this instead of findAllById() anywhere products are returned in a
        * response,
        * otherwise Product.category stays as an unloaded LAZY proxy and causes
        * "no session" error when Jackson serializes after @Transactional ends.
        */
       @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category WHERE p.id IN :ids")
       List<Product> findAllByIdWithCategory(@Param("ids") List<UUID> ids);

       /**
        * Search by name or SKU â€“ JOIN FETCH category in the same query.
        */
       @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category WHERE " +
                     "(LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
                     "LOWER(p.sku) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
                     "LOWER(p.barcode) LIKE LOWER(CONCAT('%', :query, '%')))")
       List<Product> searchByNameOrSku(@Param("query") String query);

       /**
        * Low stock â€“ JOIN FETCH category so Jackson doesn't trigger N queries.
        */
       @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category " +
                     "WHERE p.currentStock <= p.minStock")
       List<Product> findLowStockProducts();

       @Query("SELECT p FROM Product p WHERE p.currentStock <= :threshold")
       List<Product> findProductsBelowStock(@Param("threshold") BigDecimal threshold);

       @Query("SELECT COUNT(p) FROM Product p WHERE p.isActive = true")
       long countActiveProducts();

       @Query("SELECT COUNT(p) FROM Product p WHERE p.isActive = true AND p.currentStock <= p.minStock AND p.currentStock > 0")
       long countLowStockProducts();

       @Query("SELECT COUNT(p) FROM Product p WHERE p.isActive = true AND p.currentStock <= 0")
       long countOutOfStockProducts();

       @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category WHERE p.isActive = true AND p.currentStock <= 0")
       List<Product> findOutOfStockProducts();
        /**
         * Load ONLY barcode strings - for O(1) duplicate barcode detection in bulk import.
         */
        @Query("SELECT p.barcode FROM Product p WHERE p.barcode IS NOT NULL")
        List<String> findAllBarcodes();

        /**
         * Returns ALL products whose barcode is shared by 2+ products.
         * Used by the frontend 'Duplicates' button -> GET /api/products/duplicates
         */
        @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category " +
               "WHERE p.barcode IS NOT NULL AND p.barcode IN (" +
               "  SELECT p2.barcode FROM Product p2 " +
               "  WHERE p2.barcode IS NOT NULL " +
               "  GROUP BY p2.barcode HAVING COUNT(p2) > 1" +
               ") ORDER BY p.barcode, p.name")
        List<Product> findProductsWithDuplicateBarcodes();
}
