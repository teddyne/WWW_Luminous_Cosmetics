package com.luminous.service.admin;

import com.luminous.dto.ReviewRow;
import com.luminous.model.OrderDetail;
import com.luminous.model.Product;
import com.luminous.model.ProductVariant;
import com.luminous.model.Review;
import com.luminous.model.User;
import com.luminous.repository.UserRepository;
import com.luminous.repository.review.ReviewRepository;
import com.luminous.service.ProductImage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AdminReviewService {

    private final ReviewRepository reviews;
    private final UserRepository users;

    public AdminReviewService(ReviewRepository reviews, UserRepository users) {
        this.reviews = reviews;
        this.users = users;
    }

    public Page<ReviewRow> page(Byte rating, Boolean visible, String q, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Review> result = reviews.findAll(spec(rating, visible, q), pageable);
        return result.map(this::toRow);
    }

    private Specification<Review> spec(Byte rating, Boolean visible, String q) {
        return (root, query, cb) -> {
            List<Predicate> and = new ArrayList<>();
            if (rating != null) {
                and.add(cb.equal(root.get("rating"), rating));
            }
            if (visible != null) {
                and.add(cb.equal(root.get("isVisible"), visible));
            }
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                Join<Review, OrderDetail> detail = root.join("orderDetail", JoinType.LEFT);
                Join<OrderDetail, ProductVariant> variant = detail.join("variant", JoinType.LEFT);
                Join<ProductVariant, Product> product = variant.join("product", JoinType.LEFT);
                query.distinct(true);
                and.add(cb.or(
                        cb.like(cb.lower(product.get("name")), like),
                        cb.like(cb.lower(root.get("comment")), like)));
            }
            return and.isEmpty() ? null : cb.and(and.toArray(Predicate[]::new));
        };
    }

    @Transactional
    public int bulkDelete(List<Integer> ids) {
        int deleted = 0;
        for (Integer id : ids) {
            if (reviews.existsById(id)) {
                reviews.deleteById(id);
                deleted++;
            }
        }
        return deleted;
    }

    @Transactional
    public int bulkSetVisible(List<Integer> ids, boolean visible) {
        int changed = 0;
        for (Integer id : ids) {
            Review review = reviews.findById(id).orElse(null);
            if (review == null || Boolean.valueOf(visible).equals(review.getIsVisible())) {
                continue;
            }
            review.setIsVisible(visible);
            reviews.save(review);
            changed++;
        }
        return changed;
    }

    @Transactional
    public void toggleVisible(Integer id) {
        Review review = reviews.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá"));
        review.setIsVisible(!Boolean.TRUE.equals(review.getIsVisible()));
        reviews.save(review);
    }

    @Transactional
    public void delete(Integer id) {
        reviews.deleteById(id);
    }

    private ReviewRow toRow(Review review) {
        var product = review.getOrderDetail().getVariant().getProduct();
        String customerName = users.findById(review.getUserId())
                .map(User::getFullName).orElse("Khách hàng");
        return new ReviewRow(
                review.getId(),
                customerName,
                product.getName(),
                ProductImage.imageFor(product),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt(),
                Boolean.TRUE.equals(review.getIsVisible()),
                Boolean.TRUE.equals(review.getIsAnonymous()));
    }
}
