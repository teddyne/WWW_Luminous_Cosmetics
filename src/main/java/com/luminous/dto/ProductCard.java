package com.luminous.dto;

import com.luminous.model.Product;
import com.luminous.model.ProductVariant;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ProductCard {
    private final Product product;
    private final ProductVariant variant;
    private final String imageUrl;
    private final double rating;      // 0 neu chua co review
    private final long reviewCount;
    private final long soldCount;
    private final BigDecimal effectivePrice;

    public ProductCard(Product product, ProductVariant variant, String imageUrl,
                       double rating, long reviewCount, long soldCount) {
        this(product, variant, imageUrl, rating, reviewCount, soldCount, null);
    }

    public ProductCard(Product product, ProductVariant variant, String imageUrl,
                       double rating, long reviewCount, long soldCount, BigDecimal effectivePrice) {
        this.product = product;
        this.variant = variant;
        this.imageUrl = imageUrl;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.soldCount = soldCount;
        this.effectivePrice = effectivePrice;
    }

    public ProductCard withEffectivePrice(BigDecimal effectivePrice) {
        return new ProductCard(product, variant, imageUrl, rating, reviewCount, soldCount, effectivePrice);
    }

    public Product getProduct() {
        return product;
    }

    public ProductVariant getVariant() {
        return variant;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public double getRating() {
        return rating;
    }

    public long getReviewCount() {
        return reviewCount;
    }

    public long getSoldCount() {
        return soldCount;
    }

    public String brand() {
        return product.getBrand() != null ? product.getBrand() : "";
    }

    public String name() {
        return product.getName();
    }

    public String sizeLabel() {
        if (variant == null) return "";
        if (variant.getShadeName() != null && !variant.getShadeName().isBlank()) return variant.getShadeName();
        return variant.getSize() != null ? variant.getSize() : "";
    }

    public BigDecimal price() {
        return variant != null ? variant.getPrice() : null;
    }

    public BigDecimal salePrice() {
        return variant != null ? variant.getSalePrice() : null;
    }

    public BigDecimal displayPrice() {
        if (effectivePrice != null) {
            return effectivePrice;
        }
        return variant != null ? variant.displayPrice() : null;
    }

    public boolean onSale() {
        BigDecimal display = displayPrice();
        BigDecimal original = price();
        return display != null && original != null && display.compareTo(original) < 0;
    }

    public int discountPercent() {
        return discountPercentOf(price(), displayPrice());
    }

    public static int discountPercentOf(BigDecimal original, BigDecimal effective) {
        if (original == null || effective == null || original.compareTo(BigDecimal.ZERO) <= 0
                || effective.compareTo(original) >= 0) {
            return 0;
        }
        return BigDecimal.ONE.subtract(effective.divide(original, 4, RoundingMode.HALF_UP))
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP).intValue();
    }

    public int ratingStars() {
        return (int) Math.round(rating);
    }

    /** Dinh dang tien Viet: 245.000đ (khong de logic dinh dang trong template). */
    public String displayPriceText() {
        return format(displayPrice());
    }

    /**
     * Khoang gia hien thi tren trang San pham, tinh theo gia hien thi
     * (gia sale neu co) cua cac variant dang ban: "122.000đ - 399.000đ".
     * Chi co 1 muc gia thi hien 1 gia nhu binh thuong.
     */
    public String priceRangeText() {
        List<BigDecimal> prices = product.getVariants().stream()
                .filter(v -> Boolean.TRUE.equals(v.getIsActive()))
                .map(ProductVariant::displayPrice)
                .filter(Objects::nonNull)
                .sorted()
                .toList();
        if (prices.isEmpty()) {
            return displayPriceText();
        }
        BigDecimal min = prices.get(0);
        BigDecimal max = prices.get(prices.size() - 1);
        if (min.compareTo(max) == 0) {
            return format(min);
        }
        return format(min) + " - " + format(max);
    }

    public String priceText() {
        return format(price());
    }

    private String format(BigDecimal value) {
        if (value == null) {
            return "";
        }
        return NumberFormat.getInstance(new Locale("vi", "VN")).format(value) + "đ";
    }
}
