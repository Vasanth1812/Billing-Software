package com.Billing_System.vendor.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request body for POST /api/vendors/{id}/products
 * Adds a product to the vendor's own catalog (vendor_products table).
 * This is the vendor's representation of the product — their SKU, price, unit.
 * It is NOT the same as the store's products table.
 * Link to store product via mappedProductId (optional, set during GRN).
 */
@Data
public class VendorProductRequestDTO {

    /** Vendor's own product name (e.g. "Amul Butter 500g") */
    @NotBlank(message = "Product name is required")
    @Size(max = 255, message = "Product name must not exceed 255 characters")
    private String productName;

    /** Vendor's internal SKU / item code (unique per vendor) */
    @NotBlank(message = "Vendor SKU is required")
    @Size(max = 100, message = "Vendor SKU must not exceed 100 characters")
    private String vendorSku;

    /** Cost price from vendor per unit */
    @NotNull(message = "Purchase price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Purchase price must be greater than 0")
    private BigDecimal purchasePrice;

    /** Unit of measure: KG, PCS, LTR, BOX, etc. */
    @NotBlank(message = "Unit of measure is required")
    @Size(max = 20, message = "Unit must not exceed 20 characters")
    private String unitOfMeasure;

    /** Optional: pack description, e.g. "500ml", "12pcs", "1kg" */
    @Size(max = 50, message = "Pack size must not exceed 50 characters")
    private String packSize;

    /** GST rate: 0 / 5 / 12 / 18 / 28 */
    @DecimalMin(value = "0.0", message = "GST rate cannot be negative")
    @DecimalMax(value = "100.0", message = "GST rate cannot exceed 100")
    private BigDecimal gstRate;

    /** HSN code for GST classification */
    @Size(max = 20, message = "HSN code must not exceed 20 characters")
    private String hsnCode;

    /** Brand name (Amul, Nestle, ITC, etc.) */
    @Size(max = 100, message = "Brand must not exceed 100 characters")
    private String brand;

    /** Category (Dairy, Beverages, Snacks, etc.) */
    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;

    /** Minimum order quantity the vendor accepts */
    @DecimalMin(value = "0.0", message = "Minimum order quantity cannot be negative")
    private BigDecimal minOrderQty;

    /** Product description / notes */
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    /**
     * Optional: link to store's product table (UUID from products table).
     * Usually set later during GRN matching. Leave null on creation.
     */
    private UUID mappedProductId;
}
