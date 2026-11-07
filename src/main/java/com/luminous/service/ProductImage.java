package com.luminous.service;

import com.luminous.model.Product;
import com.luminous.model.ProductMedia;
import com.luminous.model.ProductVariant;
import java.util.Comparator;

public final class ProductImage {

    private ProductImage() {
    }

    public static String imageFor(Product product) {
        return imageFor(product, product == null ? java.util.List.of() : product.getVariants());
    }

    public static String imageFor(Product product, java.util.List<ProductVariant> variants) {
        if (product == null) {
            return "/images/placeholder.svg";
        }
        ProductVariant variant = firstVariant(variants);
        String url = variant != null ? variant.getImgUrl() : null;
        if (url != null && !url.isBlank() && !url.contains("example.com")) {
            return url;
        }
        for (ProductMedia media : product.getMedia()) {
            if (!"IMAGE".equals(media.getMediaType())) {
                continue;
            }
            String mediaUrl = media.getThumbnailUrl() != null ? media.getThumbnailUrl() : media.getMediaUrl();
            if (mediaUrl != null && !mediaUrl.contains("example.com")) {
                return mediaUrl;
            }
        }
        return "/images/placeholder.svg";
    }

    public static ProductVariant firstVariant(Product product) {
        return firstVariant(product.getVariants());
    }

    public static ProductVariant firstVariant(java.util.List<ProductVariant> variants) {
        return variants.stream()
                .filter(v -> Boolean.TRUE.equals(v.getIsActive()))
                .sorted(Comparator.comparing((ProductVariant v) -> !v.onSale())
                        .thenComparing(ProductVariant::getPrice, Comparator.nullsLast(Comparator.naturalOrder())))
                .findFirst()
                .orElse(null);
    }
}
