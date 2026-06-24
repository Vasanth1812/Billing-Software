package com.Billing_System.vendor.controller;

import com.Billing_System.vendor.entity.VendorCategory;
import com.Billing_System.vendor.repository.VendorCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vendor-categories")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class VendorCategoryController {

    private final VendorCategoryRepository vendorCategoryRepository;

    @GetMapping
    public ResponseEntity<List<VendorCategory>> getAllCategories() {
        return ResponseEntity.ok(vendorCategoryRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<VendorCategory> createCategory(@RequestBody VendorCategory category) {
        if (category.getColor() == null) {
            category.setColor("#3b82f6"); // default blue
        }
        return ResponseEntity.ok(vendorCategoryRepository.save(category));
    }
}
