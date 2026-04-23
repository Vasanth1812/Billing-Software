package com.Billing_System.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 100, unique = true)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @JsonProperty("selling_price")
    @Column(name = "selling_price", precision = 10, scale = 2)
    private BigDecimal sellingPrice;

    @JsonProperty("gst_rate")
    @Column(name = "gst_rate", precision = 5, scale = 2)
    private BigDecimal gstRate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Category() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(BigDecimal sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public BigDecimal getGstRate() {
        return gstRate;
    }

    public void setGstRate(BigDecimal gstRate) {
        this.gstRate = gstRate;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static CategoryBuilder builder() {
        return new CategoryBuilder();
    }

    public static class CategoryBuilder {
        private String name;
        private String description;
        private BigDecimal sellingPrice;
        private BigDecimal gstRate;

        public CategoryBuilder name(String name) {
            this.name = name;
            return this;
        }

        public CategoryBuilder description(String description) {
            this.description = description;
            return this;
        }

        public CategoryBuilder sellingPrice(BigDecimal sellingPrice) {
            this.sellingPrice = sellingPrice;
            return this;
        }

        public CategoryBuilder gstRate(BigDecimal gstRate) {
            this.gstRate = gstRate;
            return this;
        }

        public Category build() {
            Category c = new Category();
            c.setName(name);
            c.setDescription(description);
            c.setSellingPrice(sellingPrice);
            c.setGstRate(gstRate);
            return c;
        }
    }
}
