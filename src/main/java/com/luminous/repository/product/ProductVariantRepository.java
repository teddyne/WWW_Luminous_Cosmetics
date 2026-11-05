package com.luminous.repository.product;

import com.luminous.model.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Integer> {

    @Query("SELECT v FROM ProductVariant v WHERE v.product.category.id = :categoryId "
            + "AND v.isActive = true AND v.stockQuantity > :stock")
    List<ProductVariant> findByProductCategoryIdAndIsActiveTrueAndStockQuantityGreaterThan(
            @Param("categoryId") Integer categoryId, @Param("stock") Integer stock);

    List<ProductVariant> findByProductIdIn(List<Integer> productIds);

    List<ProductVariant> findByProductIdOrderByPriceAsc(Integer productId);

    Optional<ProductVariant> findFirstByProductIdOrderByPriceAsc(Integer productId);

    boolean existsBySkuIgnoreCase(String sku);

    void deleteByProductId(Integer productId);
}
