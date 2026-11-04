package com.luminous.service;

import com.luminous.dto.ProductCard;
import com.luminous.model.Product;
import com.luminous.model.ProductMedia;
import com.luminous.model.ProductVariant;
import com.luminous.repository.OrderDetailRepository;
import com.luminous.repository.ProductRepository;
import com.luminous.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ProductService {
    private final ProductRepository products;
    private final OrderDetailRepository orderDetails;
    private final ReviewRepository reviews;

    /** Anh cho san chung khi san pham chua co anh nao (khong do ve anh san pham khac). */
    private static final String NO_IMAGE = "/images/products/no-image.svg";

    public ProductService(ProductRepository products,
                          OrderDetailRepository orderDetails,
                          ReviewRepository reviews) {
        this.products = products;
        this.orderDetails = orderDetails;
        this.reviews = reviews;
    }

    public List<ProductCard> deals(int limit) {
        return toCards(products.findDeals(), limit);
    }

    public List<ProductCard> newest(int limit) {
        return toCards(products.findByIsActiveTrueOrderByCreatedAtDesc(), limit);
    }

    public List<ProductCard> bestSellers(int limit) {
        List<Product> best = orderDetails.findBestSellers();
        if (best.isEmpty()) {
            return newest(limit);
        }
        return toCards(best.stream().filter(p -> Boolean.TRUE.equals(p.getIsActive())).toList(), limit);
    }

    public List<ProductCard> allActiveCards() {
        return toCards(products.findByIsActiveTrueOrderByCreatedAtDesc(), Integer.MAX_VALUE);
    }

    public List<ProductCard> search(String q, String brand, List<Integer> categoryIds,
                                    BigDecimal minPrice, BigDecimal maxPrice,
                                    Double minRating, String sort, boolean onSaleOnly) {
        List<ProductCard> result = allActiveCards().stream()
                .filter(c -> q == null || q.isBlank()
                        || c.name().toLowerCase().contains(q.toLowerCase())
                        || c.brand().toLowerCase().contains(q.toLowerCase()))
                .filter(c -> brand == null || brand.isBlank() || c.brand().equalsIgnoreCase(brand))
                .filter(c -> categoryIds == null || categoryIds.isEmpty()
                        || (c.getProduct().getCategory() != null
                        && categoryIds.contains(c.getProduct().getCategory().getId())))
                .filter(c -> (minPrice == null && maxPrice == null)
                        || hasVariantInPriceRange(c, minPrice, maxPrice))
                .filter(c -> minRating == null || c.getRating() >= minRating)
                .filter(c -> !onSaleOnly || c.onSale())
                .collect(Collectors.toList());

        if ("price_asc".equals(sort)) {
            result.sort(Comparator.comparing(ProductCard::displayPrice,
                    Comparator.nullsLast(Comparator.naturalOrder())));
        } else if ("price_desc".equals(sort)) {
            result.sort(Comparator.comparing(ProductCard::displayPrice,
                    Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        } else if ("best".equals(sort)) {
            result.sort(Comparator.comparingLong(ProductCard::getSoldCount).reversed());
        } else if ("newest".equals(sort)) {
            result.sort(Comparator.comparing((ProductCard c) -> c.getProduct().getCreatedAt(),
                    Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        }
        // sort rong/khong hop le: giu nguyen thu tu mac dinh (moi nhat truoc).
        return result;
    }

    private boolean hasVariantInPriceRange(ProductCard card, BigDecimal min, BigDecimal max) {
        return card.getProduct().getVariants().stream()
                .filter(v -> Boolean.TRUE.equals(v.getIsActive()))
                .map(ProductVariant::displayPrice)
                .filter(Objects::nonNull)
                .anyMatch(p -> (min == null || p.compareTo(min) >= 0)
                        && (max == null || p.compareTo(max) <= 0));
    }

    private List<ProductCard> toCards(List<Product> list, int limit) {
        return list.stream()
                .filter(p -> firstVariant(p) != null)
                .limit(limit)
                .map(p -> {
                    ProductVariant v = firstVariant(p);
                    Double avg = reviews.averageRating(p.getId());
                    return new ProductCard(
                            p, v, imageFor(p, v),
                            avg != null ? avg : 0,
                            reviews.reviewCount(p.getId()),
                            orderDetails.soldCount(p.getId()));
                })
                .collect(Collectors.toList());
    }

    /** Variant hien thi: variant dang giam gia truoc, roi den gia thap nhat. */
    private ProductVariant firstVariant(Product p) {
        return p.getVariants().stream()
                .filter(v -> Boolean.TRUE.equals(v.getIsActive()))
                .sorted(Comparator.comparing((ProductVariant v) -> !v.onSale())
                        .thenComparing(ProductVariant::getPrice, Comparator.nullsLast(Comparator.naturalOrder())))
                .findFirst()
                .orElse(null);
    }

    /**
     * Anh dai dien cua the san pham, lay tu DB theo thu tu:
     * anh cua variant -> anh displayOrder = 1 cua san pham -> anh cho san.
     * Khong con bang tra id -> file trong code: them san pham moi chi can
     * them dong ProductMedia / imgUrl trong DB, khong sua code.
     */
    private String imageFor(Product p, ProductVariant v) {
        String url = null;
        if (v != null && v.getImgUrl() != null && !v.getImgUrl().isBlank()) {
            url = v.getImgUrl();
        } else if (!p.getMedia().isEmpty()) {
            ProductMedia m = p.getMedia().stream()
                    .filter(x -> "IMAGE".equals(x.getMediaType()))
                    .findFirst().orElse(p.getMedia().get(0));
            url = m.getThumbnailUrl() != null ? m.getThumbnailUrl() : m.getMediaUrl();
        }
        if (url == null || url.contains("example.com")) {
            return NO_IMAGE;
        }
        return url;
    }

    /** Loc the theo tu khoa (ten/thuong hieu) va danh muc, dung cho o Tim kiem + Danh muc hot. */
    public List<ProductCard> filter(List<ProductCard> cards, String q, Integer categoryId) {
        return cards.stream()
                .filter(c -> q == null || q.isBlank()
                        || c.name().toLowerCase().contains(q.toLowerCase())
                        || c.brand().toLowerCase().contains(q.toLowerCase()))
                .filter(c -> categoryId == null
                        || (c.getProduct().getCategory() != null
                        && categoryId.equals(c.getProduct().getCategory().getId())))
                .collect(Collectors.toList());
    }
}
