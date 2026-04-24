package com.Billing_System.service;

import com.Billing_System.dto.ProductDTO;
import com.Billing_System.entity.Category;
import com.Billing_System.entity.Product;
import com.Billing_System.repository.CategoryRepository;
import com.Billing_System.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    /** List all active products */
    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findAllWithCategory();
    }

    /** Get a single product by ID – JOIN FETCH category, no extra query */
    @Transactional(readOnly = true)
    public Product getProductById(UUID id) {
        return productRepository.findByIdWithCategory(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + id));
    }

    /**
     * Barcode scan lookup – exact SKU match, returns single product.
     * Used by POS screen when cashier scans a barcode.
     * Returns 404 immediately if SKU not found or product inactive.
     */
    @Transactional(readOnly = true)
    public Product getProductBySku(String sku) {
        return productRepository.findBySkuWithCategory(sku)
                .orElseThrow(() -> new IllegalArgumentException("No active product found with barcode/SKU: " + sku));
    }

    /** Search products by name or SKU */
    @Transactional(readOnly = true)
    public List<Product> searchProducts(String query) {
        return productRepository.searchByNameOrSku(query);
    }

    /** Get all products below minimum stock level */
    @Transactional(readOnly = true)
    public List<Product> getLowStockProducts() {
        return productRepository.findLowStockProducts();
    }

    /** Create a new product – currentStock starts at 0 as per document spec */
    public Product createProduct(ProductDTO dto) {
        if (productRepository.existsBySku(dto.getSku())) {
            throw new IllegalArgumentException("Product with SKU '" + dto.getSku() + "' already exists");
        }

        Category category = null;
        if (dto.getCategoryId() != null) {
            category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found: " + dto.getCategoryId()));
        }

        Product product = Product.builder()
                .name(dto.getName())
                .sku(dto.getSku())
                .category(category)
                .unit(dto.getUnit())
                .purchaseRate(dto.getPurchaseRate())
                .mrp(dto.getMrp())
                .gstRate(dto.getGstRate())
                .hsnCode(dto.getHsnCode())
                .barcode(dto.getBarcode())
                .description(dto.getDescription())
                .minStock(dto.getMinStock())
                .currentStock(dto.getStock() != null ? dto.getStock() : java.math.BigDecimal.ZERO)
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();

        return productRepository.save(product);
    }

    /**
     * Update product details (does NOT touch currentStock – that goes through
     * purchases/sales)
     */
    public Product updateProduct(UUID id, ProductDTO dto) {
        Product product = getProductById(id);

        // Check SKU uniqueness only if it has changed
        if (!product.getSku().equals(dto.getSku()) && productRepository.existsBySku(dto.getSku())) {
            throw new IllegalArgumentException("Product with SKU '" + dto.getSku() + "' already exists");
        }

        Category category = null;
        if (dto.getCategoryId() != null) {
            category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found: " + dto.getCategoryId()));
        }

        product.setName(dto.getName());
        product.setSku(dto.getSku());
        product.setCategory(category);
        product.setUnit(dto.getUnit());
        product.setPurchaseRate(dto.getPurchaseRate());
        product.setMrp(dto.getMrp());
        product.setGstRate(dto.getGstRate());
        product.setHsnCode(dto.getHsnCode());
        product.setBarcode(dto.getBarcode());
        product.setDescription(dto.getDescription());
        product.setMinStock(dto.getMinStock());

        if (dto.getStock() != null) {
            product.setCurrentStock(dto.getStock());
        }
        if (dto.getIsActive() != null) {
            product.setIsActive(dto.getIsActive());
        }

        return productRepository.save(product);
    }

    /** Soft delete – sets isActive to false */
    public void deleteProduct(UUID id) {
        Product product = getProductById(id);
        product.setIsActive(false);
        productRepository.save(product);
    }
}
