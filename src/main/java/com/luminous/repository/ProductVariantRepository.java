package com.luminous.repository;

import com.luminous.model.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Integer> {
    List<ProductVariant> findByProductIdAndIsActiveTrue(Integer productId);

    /** Variant con hang, dang ban, cua 1 danh muc (de chon buoc lich + doi san pham).
     *  Dung JPQL tuong minh thay vi ten method dai, tranh Spring Data doan sai
     *  duong product -> category -> id. */
    @Query("SELECT v FROM ProductVariant v WHERE v.product.category.id = :categoryId " +
           "AND v.isActive = true AND v.stockQuantity > :stock")
    List<ProductVariant> findByProductCategoryIdAndIsActiveTrueAndStockQuantityGreaterThan(
            @Param("categoryId") Integer categoryId, @Param("stock") Integer stock);
}
