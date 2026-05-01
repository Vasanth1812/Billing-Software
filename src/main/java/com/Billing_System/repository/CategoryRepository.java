package com.Billing_System.repository;

import com.Billing_System.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByIsActiveTrue();

    Optional<Category> findByName(String name);

    /** Case-insensitive lookup — used during bulk import so "Dairy" and "dairy" map to same category */
    Optional<Category> findByNameIgnoreCase(String name);

    boolean existsByName(String name);
}
