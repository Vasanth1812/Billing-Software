package com.Billing_System.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ProductDTO {

    @NotBlank(message = "Product name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    @NotBlank(message = "SKU/barcode is required")
    @Size(max = 50, message = "SKU must not exceed 50 characters")
    private String sku;

    private UUID categoryId;

    @Size(max = 20, message = "Unit must not exceed 20 characters")
    private String unit;

    @DecimalMin(value = "0.0", message = "Purchase rate cannot be negative")
    private BigDecimal purchaseRate;

    @DecimalMin(value = "0.0", message = "MRP cannot be negative")
    private BigDecimal mrp;

    @DecimalMin(value = "0.0", message = "GST rate cannot be negative")
    @DecimalMax(value = "100.0", message = "GST rate cannot exceed 100%")
    private BigDecimal gstRate;

    @Size(max = 20, message = "HSN code must not exceed 20 characters")
    private String hsnCode;

    private String barcode;

    @Size(max = 100, message = "Brand must not exceed 100 characters")
    private String brand;

    @DecimalMin(value = "0.0", message = "Selling price cannot be negative")
    private BigDecimal sellingPrice;

    private String description;

    @JsonProperty("currentStock")
    @DecimalMin(value = "0.0", message = "Stock cannot be negative")
    private BigDecimal currentStock;

    @JsonProperty("minStock")
    @DecimalMin(value = "0.0", message = "Minimum stock cannot be negative")
    private BigDecimal minStock;

    private Boolean isActive;
}
