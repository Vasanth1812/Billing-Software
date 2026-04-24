package com.Billing_System.service;

import com.Billing_System.dto.InventoryProductDTO;
import com.Billing_System.dto.InventorySummaryDTO;
import com.Billing_System.dto.StockAdjustmentDTO;
import com.Billing_System.entity.Product;
import com.Billing_System.entity.StockLedger;
import com.Billing_System.repository.ProductRepository;
import com.Billing_System.repository.StockLedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryService {

    private final ProductRepository productRepository;
    private final StockLedgerRepository stockLedgerRepository;

    @Transactional(readOnly = true)
    public InventorySummaryDTO getInventorySummary() {
        return InventorySummaryDTO.builder()
                .totalSkus(productRepository.countActiveProducts())
                .lowStockCount(productRepository.countLowStockProducts())
                .outOfStockCount(productRepository.countOutOfStockProducts())
                .build();
    }

    @Transactional(readOnly = true)
    public List<InventoryProductDTO> getInventoryProducts(String status) {
        List<Product> products;
        if ("LOW".equalsIgnoreCase(status)) {
            products = productRepository.findLowStockProducts();
        } else if ("OUT".equalsIgnoreCase(status)) {
            products = productRepository.findOutOfStockProducts();
        } else {
            products = productRepository.findAllWithCategory();
        }

        return products.stream()
                .map(this::mapToInventoryDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    private InventoryProductDTO mapToInventoryDTO(Product product) {
        String stockStatus = "OK";
        BigDecimal current = product.getCurrentStock() != null ? product.getCurrentStock() : BigDecimal.ZERO;
        BigDecimal min = product.getMinStock() != null ? product.getMinStock() : BigDecimal.ZERO;

        if (current.compareTo(BigDecimal.ZERO) <= 0) {
            stockStatus = "OUT";
        } else if (current.compareTo(min) <= 0) {
            stockStatus = "LOW";
        }

        double level = 100.0;
        if (min.compareTo(BigDecimal.ZERO) > 0) {
            level = current.divide(min, 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100.0;
            if (level > 100.0)
                level = 100.0;
        } else if (current.compareTo(BigDecimal.ZERO) <= 0) {
            level = 0.0;
        }

        return InventoryProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .sku(product.getSku())
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : "Uncategorized")
                .currentStock(current)
                .minStock(min)
                .status(stockStatus)
                .level(level)
                .build();
    }

    /**
     * Manually adjust stock levels.
     * Records a 'ADJUST' entry in the stock ledger.
     */
    @Transactional
    public void adjustStock(StockAdjustmentDTO dto) {
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + dto.getProductId()));

        // Ensure amount is positive from DTO, we determine the sign by 'direction'
        BigDecimal amount = dto.getQuantity().abs();
        String direction = dto.getDirection() != null ? dto.getDirection().trim() : "+";

        // Logic: direction "-" or "OUT" means we decrease stock
        boolean isSubtraction = "-".equals(direction) ||
                "OUT".equalsIgnoreCase(direction) ||
                "SUBTRACT".equalsIgnoreCase(direction) ||
                "REMOVE".equalsIgnoreCase(direction);

        if (isSubtraction) {
            amount = amount.negate();
        }

        BigDecimal oldStock = product.getCurrentStock() != null ? product.getCurrentStock() : BigDecimal.ZERO;
        BigDecimal newStock = oldStock.add(amount);

        product.setCurrentStock(newStock);
        productRepository.save(product);

        // Record in ledger
        StockLedger ledger = StockLedger.builder()
                .product(product)
                .transactionType("ADJUST")
                .referenceId(null)
                .quantityIn(!isSubtraction ? amount : BigDecimal.ZERO)
                .quantityOut(isSubtraction ? amount.abs() : BigDecimal.ZERO)
                .balanceStock(newStock)
                .transactionDate(LocalDateTime.now())
                .reason(dto.getReason() != null ? dto.getReason() : "Manual Adjustment")
                .build();

        stockLedgerRepository.save(ledger);
    }

    @Transactional(readOnly = true)
    public List<StockLedger> getStockHistory(UUID productId) {
        return stockLedgerRepository.findByProductIdOrderByTransactionDateDesc(productId);
    }
}
