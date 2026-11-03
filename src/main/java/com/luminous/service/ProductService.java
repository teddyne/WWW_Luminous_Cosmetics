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

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ProductService {
    private final ProductRepository products;
    private final OrderDetailRepository orderDetails;
    private final ReviewRepository reviews;

    private static final Map<Integer, String> LOCAL_IMAGES = Map.of(
            1, "/images/products/nuoc-tay-trang-cocoon-bi-dao-1.jpg",        // Cocoon tay trang
            2, "/images/products/sua-rua-mat-cerave-sach-sau-2.jpg",         // CeraVe sua rua mat
            3, "/images/products/sua-rua-mat-cetaphil-diu-nhe-3.jpg",        // Cetaphil sua rua mat
            4, "/images/products/serum-the-ordinary-niacinamide-4.jpg",      // Ordinary Niacinamide
            5, "/images/products/serum-klairs-vitamin-c-5.jpg",              // Klairs Vitamin C
            6, "/images/products/kem-chong-nang-la-roche-posay-anthelios-6.jpg", // La Roche-Posay Anthelios
            7, "/images/products/gel-chong-nang-skin-aqua-uv-7.jpg",         // Skin Aqua gel
            8, "/images/products/kem-duong-am-cerave-moisturising-8.jpg"     // CeraVe duong am
    );

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
        // Neu DB chua co don hang nao, best sellers se rong -> fallback ve san pham moi nhat
        // de trang chu khong bi khoi trong (van la du lieu that tu DB, khong phai so gia).
        if (best.isEmpty()) {
            return newest(limit);
        }
        return toCards(best.stream().filter(p -> Boolean.TRUE.equals(p.getIsActive())).toList(), limit);
    }

    public List<ProductCard> allActiveCards() {
        return toCards(products.findByIsActiveTrueOrderByCreatedAtDesc(), Integer.MAX_VALUE);
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
        // URL mau example.com khong tai duoc -> doi sang anh local; URL that giu nguyen.
        if (url == null || url.contains("example.com")) {
            return LOCAL_IMAGES.getOrDefault(p.getId(), "/images/products/nuoc-tay-trang-cocoon-bi-dao-1.jpg");
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
