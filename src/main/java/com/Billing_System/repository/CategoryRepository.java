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

    boolean existsByName(String name);
}
