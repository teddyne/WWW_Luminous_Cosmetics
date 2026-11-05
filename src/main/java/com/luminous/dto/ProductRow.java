package com.luminous.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductRow(
        Integer id,
        String name,
        String brand,
        String categoryName,
        String imageUrl,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        int totalStock,
        long soldCount,
        boolean active,
        LocalDateTime createdAt,
        int variantCount,
        Integer firstVariantId) {

    public String minPriceText() {
        return Vnd.format(minPrice);
    }

    public String priceText() {
        if (maxPrice != null && minPrice != null && maxPrice.compareTo(minPrice) > 0) {
            return Vnd.format(minPrice) + " - " + Vnd.format(maxPrice);
        }
        return Vnd.format(minPrice);
    }
}
