package com.luminous.repository;

import com.luminous.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    List<Product> findByIsActiveTrueOrderByCreatedAtDesc();

    List<Product> findByIsActiveTrueAndCategoryIdOrderByCreatedAtDesc(Integer categoryId);

    /** San pham co it nhat 1 variant dang giam gia. */
    @Query("SELECT DISTINCT p FROM Product p JOIN p.variants v " +
           "WHERE p.isActive = true AND v.isActive = true AND v.salePrice IS NOT NULL " +
           "ORDER BY p.createdAt DESC")
    List<Product> findDeals();
}
