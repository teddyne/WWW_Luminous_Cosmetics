package com.luminous.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Ban sao phang cua ProductCard de luu Redis dang JSON (khong luu entity
 * JPA: entity mang quan he lazy, khong serialize on dinh, va gia/ton kho
 * trong cache chi dung de hien thi, khong dung de quyet dinh dat hang).
 *
 * createdAt luu dang chuoi ISO-8601 (vd "2026-11-16T10:15:30") thay vi
 * LocalDateTime: bo chuyen JSON com.fasterxml tren may user khong co module
 * jackson-datatype-jsr310 nen khong ghi duoc LocalDateTime, lam toan bo
 * luu cache that bai (loi "Java 8 date/time type not supported").
 * Chuoi thi Jackson nao cung ghi/doc duoc, khong can them dependency.
 */
public record CachedProduct(
        Integer productId,
        Integer variantId,
        String name,
        String brand,
        Integer categoryId,
        String categoryName,
        BigDecimal price,
        BigDecimal salePrice,
        Integer stockQuantity,
        String imageUrl,
        double rating,
        long reviewCount,
        long soldCount,
        String description,
        String usageInstructions,
        List<String> mediaUrls,
        List<BigDecimal> variantPrices,
        String createdAt,
        String size,
        String shadeName
) {
    public CachedProduct {
        mediaUrls = mediaUrls == null ? List.of() : List.copyOf(mediaUrls);
        variantPrices = variantPrices == null ? List.of() : List.copyOf(variantPrices);
    }
}
