package com.luminous.repository.review;

import com.luminous.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /** Danh gia hien thi cua 1 san pham, moi nhat truoc (dung cho trang chi tiet). */
    @Query("SELECT r FROM Review r WHERE r.isVisible = true AND r.orderDetail.variant.product.id = :productId ORDER BY r.createdAt DESC")
    Page<Review> findVisibleByProduct(@Param("productId") Integer productId, Pageable pageable);

    /** Danh gia hien thi cua 1 san pham, loc theo so sao. */
    @Query("SELECT r FROM Review r WHERE r.isVisible = true AND r.orderDetail.variant.product.id = :productId AND r.rating = :rating ORDER BY r.createdAt DESC")
    Page<Review> findVisibleByProductAndRating(@Param("productId") Integer productId,
                                               @Param("rating") byte rating, Pageable pageable);

    /** So luong danh gia theo tung muc sao: moi dong [rating, count]. */
    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.isVisible = true AND r.orderDetail.variant.product.id = :productId GROUP BY r.rating")
    List<Object[]> countByRating(@Param("productId") Integer productId);

}
