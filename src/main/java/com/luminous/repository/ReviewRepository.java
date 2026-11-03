package com.luminous.repository;

import com.luminous.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Integer> {
    /** Diem trung binh chi tinh review dang hien. Tra null neu chua co review. */
    @Query("SELECT AVG(r.rating) FROM Review r " +
           "WHERE r.isVisible = true AND r.orderDetail.variant.product.id = :productId")
    Double averageRating(@Param("productId") Integer productId);

    @Query("SELECT COUNT(r) FROM Review r " +
           "WHERE r.isVisible = true AND r.orderDetail.variant.product.id = :productId")
    long reviewCount(@Param("productId") Integer productId);
}
