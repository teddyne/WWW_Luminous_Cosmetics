package com.luminous.dto.ai;

import com.luminous.dto.ProductCard;

import java.math.BigDecimal;

public record AiProductView(Integer id, String name, String brand, String imageUrl, String url,
                            BigDecimal price, BigDecimal displayPrice, String priceText,
                            String displayPriceText, boolean onSale, Integer stock,
                            double rating, long reviewCount) {

    public static AiProductView from(ProductCard card) {
        Integer stock = card.getVariant() != null ? card.getVariant().getStockQuantity() : null;
        return new AiProductView(
                card.getProduct().getId(),
                card.name(),
                card.brand(),
                card.getImageUrl(),
                "/products/" + card.getProduct().getId(),
                card.price(),
                card.displayPrice(),
                card.priceText(),
                card.displayPriceText(),
                card.onSale(),
                stock,
                card.getRating(),
                card.getReviewCount());
    }
}
