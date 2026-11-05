package com.luminous.repository.product;

import com.luminous.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Integer>, JpaSpecificationExecutor<Product> {

    List<Product> findByIsActiveTrueOrderByCreatedAtDesc();

    List<Product> findByIsActiveTrueAndCategoryIdOrderByCreatedAtDesc(Integer categoryId);

    List<Product> findAllByOrderByNameAsc();

    List<Product> findByIsActiveTrueOrderByNameAsc();

    long countByCategoryId(Integer categoryId);

    long countByIsActiveTrue();

    @Query("SELECT DISTINCT p FROM Product p JOIN p.variants v WHERE p.isActive = true AND v.isActive = true AND v.salePrice IS NOT NULL ORDER BY p.createdAt DESC")
    List<Product> findDeals();

}
