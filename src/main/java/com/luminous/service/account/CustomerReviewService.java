package com.luminous.service.account;

import com.luminous.model.OrderDetail;
import com.luminous.model.PointTransaction;
import com.luminous.model.Review;
import com.luminous.model.ReviewMedia;
import com.luminous.model.User;
import com.luminous.repository.UserRepository;
import com.luminous.repository.order.OrderDetailRepository;
import com.luminous.repository.order.PointTransactionRepository;
import com.luminous.repository.review.ReviewMediaRepository;
import com.luminous.repository.review.ReviewRepository;
import com.luminous.service.FileStorageService;
import com.luminous.service.notification.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Khach tu danh gia san pham da mua (moi dong OrderDetail chi danh gia 1 lan).
 * Danh gia xong cong 200 diem / 1 san pham (hien "Danh gia ngay va nhan 200 Xu"
 * o trang Don mua). Kem duoc toi da 5 anh + 1 video / 1 san pham, luu vao
 * ReviewMedia, hien o trang chi tiet san pham.
 */
@Service
public class CustomerReviewService {

    public static final int REVIEW_REWARD_POINTS = 200;
    public static final int MAX_REVIEW_IMAGES = 5;

    private final ReviewRepository reviews;
    private final ReviewMediaRepository reviewMedia;
    private final OrderDetailRepository orderDetails;
    private final UserRepository users;
    private final PointTransactionRepository pointTransactions;
    private final FileStorageService fileStorage;
    private final NotificationService notifications;

    public CustomerReviewService(ReviewRepository reviews,
                                 ReviewMediaRepository reviewMedia,
                                 OrderDetailRepository orderDetails,
                                 UserRepository users,
                                 PointTransactionRepository pointTransactions,
                                 FileStorageService fileStorage,
                                 NotificationService notifications) {
        this.reviews = reviews;
        this.reviewMedia = reviewMedia;
        this.orderDetails = orderDetails;
        this.users = users;
        this.pointTransactions = pointTransactions;
        this.fileStorage = fileStorage;
        this.notifications = notifications;
    }

    @Transactional
    public Review submitReview(Integer userId, Integer orderDetailId,
                               Byte rating, String comment, boolean anonymous) {
        return submitReviewWithMedia(userId, orderDetailId, rating, comment, anonymous, null, null);
    }

    @Transactional
    public Review submitReviewWithMedia(Integer userId, Integer orderDetailId,
                                        Byte rating, String comment, boolean anonymous,
                                        List<MultipartFile> images, MultipartFile video) {
        OrderDetail detail = orderDetails.findById(orderDetailId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm trong đơn"));
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Vui lòng chọn số sao từ 1 đến 5");
        }
        if (reviews.existsByOrderDetailIdAndUserId(orderDetailId, userId)) {
            throw new IllegalArgumentException("Bạn đã đánh giá sản phẩm này rồi");
        }
        Review review = new Review();
        review.setUserId(userId);
        review.setOrderDetail(detail);
        review.setRating(rating);
        review.setComment(comment == null || comment.isBlank() ? null : comment.trim());
        review.setIsAnonymous(anonymous);
        review.setIsVisible(true);
        review.setCreatedAt(LocalDateTime.now());
        reviews.save(review);
        saveMedia(review, images, video);
        awardReviewPoints(userId, detail);
        return review;
    }

    private void saveMedia(Review review, List<MultipartFile> images, MultipartFile video) {
        int count = 0;
        if (images != null) {
            for (MultipartFile file : images) {
                if (count >= MAX_REVIEW_IMAGES) {
                    break;
                }
                if (file == null || file.isEmpty()) {
                    continue;
                }
                if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
                    throw new IllegalArgumentException("Chỉ được tải ảnh lên (định dạng ảnh)");
                }
                String url = fileStorage.store(file, "reviews", "review-" + review.getId() + "-" + count);
                if (url == null) {
                    continue;
                }
                ReviewMedia media = new ReviewMedia();
                media.setReview(review);
                media.setMediaType("IMAGE");
                media.setMediaUrl(url);
                reviewMedia.save(media);
                count++;
            }
        }
        if (video != null && !video.isEmpty()) {
            if (video.getContentType() == null || !video.getContentType().startsWith("video/")) {
                throw new IllegalArgumentException("Video tải lên phải đúng định dạng video");
            }
            String url = fileStorage.store(video, "reviews", "review-" + review.getId() + "-video");
            if (url != null) {
                ReviewMedia media = new ReviewMedia();
                media.setReview(review);
                media.setMediaType("VIDEO");
                media.setMediaUrl(url);
                reviewMedia.save(media);
            }
        }
    }

    private void awardReviewPoints(Integer userId, OrderDetail detail) {
        User user = users.findById(userId).orElse(null);
        if (user == null) {
            return;
        }
        int balance = (user.getPointBalance() == null ? 0 : user.getPointBalance()) + REVIEW_REWARD_POINTS;
        user.setPointBalance(balance);
        users.save(user);
        PointTransaction tx = new PointTransaction();
        tx.setUser(user);
        tx.setOrderId(detail.getOrderId());
        tx.setType("EARN");
        tx.setPoints(REVIEW_REWARD_POINTS);
        tx.setBalanceAfter(balance);
        tx.setDescription("Thưởng 200 điểm đánh giá sản phẩm đơn " + detail.getOrderId());
        pointTransactions.save(tx);
        notifications.notify(userId, "POINTS", "Bạn được nhận 200 Xu vào tài khoản",
                "Cảm ơn bạn đã đánh giá sản phẩm. 200 Xu đã được cộng vào tài khoản của bạn. Số dư hiện tại: " + balance + " điểm.",
                "/account/points");
    }
}
