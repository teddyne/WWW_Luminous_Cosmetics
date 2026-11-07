package com.luminous.repository.review;

import com.luminous.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Integer>, JpaSpecificationExecutor<Review> {

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.isVisible = true AND r.orderDetail.variant.product.id = :productId")
    Double averageRating(@Param("productId") Integer productId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.isVisible = true AND r.orderDetail.variant.product.id = :productId")
    long reviewCount(@Param("productId") Integer productId);

    long countByIsVisibleTrue();

    List<Review> findByUserIdOrderByCreatedAtDesc(Integer userId);

}
