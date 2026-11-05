package com.luminous.dto;

import com.luminous.model.Product;
import com.luminous.model.ProductVariant;

import java.math.BigDecimal;
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

    public ProductCard(Product product, ProductVariant variant, String imageUrl,
                       double rating, long reviewCount, long soldCount) {
        this.product = product;
        this.variant = variant;
        this.imageUrl = imageUrl;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.soldCount = soldCount;
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
        return variant != null && variant.getSize() != null ? variant.getSize() : "";
    }

    public BigDecimal price() {
        return variant != null ? variant.getPrice() : null;
    }

    public BigDecimal salePrice() {
        return variant != null ? variant.getSalePrice() : null;
    }

    public BigDecimal displayPrice() {
        return variant != null ? variant.displayPrice() : null;
    }

    public boolean onSale() {
        return variant != null && variant.onSale();
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
