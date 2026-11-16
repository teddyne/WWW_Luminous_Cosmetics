package com.luminous.dto;

import java.time.LocalDateTime;

/** Mot danh gia hien thi o trang chi tiet san pham (da an ten neu an danh). */
public record ReviewView(
        int rating,
        String comment,
        LocalDateTime createdAt,
        String reviewerName,
        java.util.List<String> imageUrls,
        java.util.List<String> videoUrls) {

    /**
     * Constructor cu (4 tham so, chua co anh/video) giu lai de tuong thich
     * voi code cu van goi new ReviewView(rating, comment, createdAt, name).
     * Neu thieu constructor nay, cho goi cu bao loi "cannot be applied"
     * va keo theo loi "List&lt;Object&gt; cannot be converted to List&lt;ReviewView&gt;"
     * o ProductService.reviewPage().
     */
    public ReviewView(int rating, String comment, LocalDateTime createdAt, String reviewerName) {
        this(rating, comment, createdAt, reviewerName, java.util.List.of(), java.util.List.of());
    }

    /** Chuoi sao hien thi, vi du "★★★★★" (tinh o Java de template khoi dung String.repeat). */
    public String stars() {
        return "★".repeat(Math.max(0, Math.min(5, rating)));
    }
}
