package com.Billing_System.service;

import com.Billing_System.dto.ProductDTO;
import com.Billing_System.entity.Category;
import com.Billing_System.entity.Product;
import com.Billing_System.entity.Supplier;
import com.Billing_System.repository.CategoryRepository;
import com.Billing_System.repository.ProductRepository;
import com.Billing_System.repository.SupplierRepository;
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
    private final SupplierRepository supplierRepository;

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

    /**
     * Returns all products whose barcode is shared by 2+ products.
     * Called by the frontend "Duplicates" button after a bulk import.
     * User can then edit each product and assign a unique barcode.
     */
    @Transactional(readOnly = true)
    public List<Product> getDuplicateBarcodeProducts() {
        return productRepository.findProductsWithDuplicateBarcodes();
    }

    /** Create a new product */
    public Product createProduct(ProductDTO dto) {
        if (productRepository.existsBySku(dto.getSku())) {
            throw new IllegalArgumentException("Product with SKU '" + dto.getSku() + "' already exists");
        }

        Category category = resolveCategory(dto.getCategoryId());
        Supplier primarySupplier = resolvePrimarySupplier(dto.getPrimarySupplierId());

        Product product = Product.builder()
                .name(dto.getName())
                .sku(dto.getSku())
                .category(category)
                .primarySupplier(primarySupplier)
                .unit(dto.getUnit())
                .purchaseRate(dto.getPurchaseRate())
                .mrp(dto.getMrp())
                .gstRate(dto.getGstRate())
                .hsnCode(dto.getHsnCode())
                .barcode(dto.getBarcode())
                .brand(dto.getBrand())
                .sellingPrice(dto.getSellingPrice())
                .description(dto.getDescription())
                .minStock(dto.getMinStock())
                .currentStock(dto.getCurrentStock() != null ? dto.getCurrentStock() : java.math.BigDecimal.ZERO)
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .build();

        return productRepository.save(product);
    }

    /** Update product details */
    public Product updateProduct(UUID id, ProductDTO dto) {
        Product product = getProductById(id);

        if (!product.getSku().equals(dto.getSku()) && productRepository.existsBySku(dto.getSku())) {
            throw new IllegalArgumentException("Product with SKU '" + dto.getSku() + "' already exists");
        }

        product.setName(dto.getName());
        product.setSku(dto.getSku());
        product.setCategory(resolveCategory(dto.getCategoryId()));
        product.setPrimarySupplier(resolvePrimarySupplier(dto.getPrimarySupplierId()));
        product.setUnit(dto.getUnit());
        product.setPurchaseRate(dto.getPurchaseRate());
        product.setMrp(dto.getMrp());
        product.setGstRate(dto.getGstRate());
        product.setHsnCode(dto.getHsnCode());
        product.setBarcode(dto.getBarcode());
        product.setBrand(dto.getBrand());
        product.setSellingPrice(dto.getSellingPrice());
        product.setDescription(dto.getDescription());
        product.setMinStock(dto.getMinStock());

        if (dto.getCurrentStock() != null) product.setCurrentStock(dto.getCurrentStock());
        if (dto.getIsActive() != null)     product.setIsActive(dto.getIsActive());

        return productRepository.save(product);
    }

    // ─── Private Helpers ────────────────────────────────────────────────────────

    private Category resolveCategory(UUID categoryId) {
        if (categoryId == null) return null;
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + categoryId));
    }

    private Supplier resolvePrimarySupplier(UUID supplierId) {
        if (supplierId == null) return null;
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found: " + supplierId));
    }

    /** Soft delete – sets isActive to false */
    public void deleteProduct(UUID id) {
        Product product = getProductById(id);
        product.setIsActive(false);
        productRepository.save(product);

    }
}
