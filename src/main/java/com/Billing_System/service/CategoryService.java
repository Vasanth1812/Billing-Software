package com.Billing_System.service;

import com.Billing_System.dto.CategoryDTO;
import com.Billing_System.entity.Category;
import com.Billing_System.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /** Get all active categories */
    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findByIsActiveTrue();
    }

    /** Get a category by ID */
    @Transactional(readOnly = true)
    public Category getCategoryById(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + id));
    }

    /** Create a new category */
    public Category createCategory(CategoryDTO dto) {
        System.out.println("Creating Category: " + dto.getName() + " | Price: " + dto.getSellingPrice() + " | GST: "
                + dto.getGstRate());

        if (categoryRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("Category with name '" + dto.getName() + "' already exists");
        }
        Category category = Category.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .sellingPrice(dto.getSellingPrice())
                .gstRate(dto.getGstRate())
                .build();
        return categoryRepository.save(category);
    }

    /** Update an existing category */
    public Category updateCategory(UUID id, CategoryDTO dto) {
        Category category = getCategoryById(id);
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());
        category.setSellingPrice(dto.getSellingPrice());
        category.setGstRate(dto.getGstRate());
        return categoryRepository.save(category);
    }

    /** Soft delete a category */
    public void deleteCategory(UUID id) {
        Category category = getCategoryById(id);
        category.setIsActive(false);
        categoryRepository.save(category);
    }
}
