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

    /** Chuoi sao hien thi, vi du "★★★★★" (tinh o Java de template khoi dung String.repeat). */
    public String stars() {
        return "★".repeat(Math.max(0, Math.min(5, rating)));
    }
}
