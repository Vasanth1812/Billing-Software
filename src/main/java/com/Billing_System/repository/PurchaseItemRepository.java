package com.Billing_System.repository;

import com.Billing_System.entity.PurchaseItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PurchaseItemRepository extends JpaRepository<PurchaseItem, UUID> {

    /**
     * JOIN FETCH product so that each PurchaseItem's product is not loaded lazily.
     * Without this, 50 items in an order = 51 queries (1 + 50 for product).
     */
    @Query("SELECT pi FROM PurchaseItem pi JOIN FETCH pi.product WHERE pi.purchaseOrder.id = :purchaseOrderId")
    List<PurchaseItem> findByPurchaseOrderId(@Param("purchaseOrderId") UUID purchaseOrderId);

    @Query("SELECT pi FROM PurchaseItem pi JOIN FETCH pi.product WHERE pi.product.id = :productId")
    List<PurchaseItem> findByProductId(@Param("productId") UUID productId);
}
