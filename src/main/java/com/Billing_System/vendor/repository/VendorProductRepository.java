package com.Billing_System.vendor.repository;

import com.Billing_System.vendor.entity.VendorProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VendorProductRepository extends JpaRepository<VendorProduct, UUID> {

    /** All active products for a specific vendor */
    List<VendorProduct> findByVendorIdAndIsActiveTrueOrderByProductNameAsc(UUID vendorId);

    /** All products for a vendor (including inactive) */
    List<VendorProduct> findByVendorIdOrderByProductNameAsc(UUID vendorId);

    /** Find by vendor + vendorSku (unique per vendor) */
    Optional<VendorProduct> findByVendorIdAndVendorSku(UUID vendorId, String vendorSku);

    /** Check if vendor SKU already exists for this vendor */
    boolean existsByVendorIdAndVendorSku(UUID vendorId, String vendorSku);

    /** All vendor products mapped to a specific store product */
    List<VendorProduct> findByMappedProductId(UUID productId);

    List<VendorProduct> findByMappedProductIdIn(List<UUID> productIds);

    /** Count products per vendor — for stats */
    long countByVendorId(UUID vendorId);

    /** All vendor SKUs for a vendor — for bulk duplicate check */
    @Query("SELECT vp.vendorSku FROM VendorProduct vp WHERE vp.vendor.id = :vendorId")
    List<String> findAllVendorSkusByVendorId(@Param("vendorId") UUID vendorId);

    /** Search vendor products by name or SKU */
    @Query("SELECT vp FROM VendorProduct vp " +
           "WHERE vp.vendor.id = :vendorId " +
           "  AND vp.isActive = true " +
           "  AND (LOWER(vp.productName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "    OR LOWER(vp.vendorSku)   LIKE LOWER(CONCAT('%', :search, '%')) " +
           "    OR LOWER(vp.brand)       LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY vp.productName ASC")
    List<VendorProduct> searchByVendor(@Param("vendorId") UUID vendorId,
                                        @Param("search") String search);

    /** All expired products for a specific vendor */
    List<VendorProduct> findByVendorIdAndExpiryDateBeforeAndIsActiveTrue(UUID vendorId, java.time.LocalDate date);

    /** Find all products with stock > 0 */
    @Query("SELECT vp FROM VendorProduct vp WHERE vp.isActive = true AND vp.currentStock > 0 ORDER BY vp.productName ASC")
    List<VendorProduct> findInStockProducts();
}
