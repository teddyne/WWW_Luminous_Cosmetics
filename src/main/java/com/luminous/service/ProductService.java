package com.luminous.service;

import com.luminous.dto.ProductCard;
import com.luminous.model.Product;
import com.luminous.model.ProductMedia;
import com.luminous.model.ProductVariant;
import com.luminous.repository.OrderDetailRepository;
import com.luminous.repository.ProductRepository;
import com.luminous.repository.ProductVariantRepository;
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
    private final ProductVariantRepository variants;

    /** Anh LOCAL theo productID trong luminous_db.sql (1 Cocoon, 2 CeraVe, ...). */
    private static final Map<Integer, String> LOCAL_IMAGES = Map.of(
            1, "/images/products/makeup-remover/nuoc-tay-trang-cocoon-bi-dao-1.jpg",        // Cocoon tay trang
            2, "/images/products/cleanser/sua-rua-mat-cerave-sach-sau-2.jpg",         // CeraVe sua rua mat
            3, "/images/products/cleanser/sua-rua-mat-cetaphil-diu-nhe-3.jpg",        // Cetaphil sua rua mat
            4, "/images/products/serum/serum-the-ordinary-niacinamide-4.jpg",      // Ordinary Niacinamide
            5, "/images/products/serum/serum-klairs-vitamin-c-5.jpg",              // Klairs Vitamin C
            6, "/images/products/sunscreen/kem-chong-nang-la-roche-posay-anthelios-6.jpg", // La Roche-Posay Anthelios
            7, "/images/products/sunscreen/gel-chong-nang-skin-aqua-uv-7.jpg",         // Skin Aqua gel
            8, "/images/products/moisturizer/kem-duong-am-cerave-moisturising-8.jpg"     // CeraVe duong am
    );

    public ProductService(ProductRepository products,
                          OrderDetailRepository orderDetails,
                          ReviewRepository reviews,
                          ProductVariantRepository variants) {
        this.products = products;
        this.orderDetails = orderDetails;
        this.reviews = reviews;
        this.variants = variants;
    }

    public List<ProductCard> deals(int limit) {
        return toCards(products.findDeals(), limit);
    }

    public List<ProductCard> newest(int limit) {
        return toCards(products.findByIsActiveTrueOrderByCreatedAtDesc(), limit);
    }

    /**
     * San pham moi nhat NHUNG loai tru cac san pham da hien o khoi uu dai.
     * Dung o HomeController de 2 khoi khong trung san pham: lay deals truoc,
     * roi lay newest trong so con lai, van du 12 san pham neu du hang.
     */
    public List<ProductCard> newestExcluding(Set<Integer> excludedProductIds, int limit) {
        if (excludedProductIds == null || excludedProductIds.isEmpty()) {
            return newest(limit);
        }
        List<Product> rest = products.findByIsActiveTrueOrderByCreatedAtDesc().stream()
                .filter(pr -> pr.getId() == null || !excludedProductIds.contains(pr.getId()))
                .toList();
        return toCards(rest, limit);
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

    /** The cho dung 1 variant (dung o chu trinh da, chi tiet, doi san pham). */
    public ProductCard cardForVariant(ProductVariant v) {
        Product p = v.getProduct();
        Double avg = reviews.averageRating(p.getId());
        return new ProductCard(
                p, v, imageForVariant(v),
                avg != null ? avg : 0,
                reviews.reviewCount(p.getId()),
                orderDetails.soldCount(p.getId()));
    }

    /** Moi variant con hang cua 1 danh muc thanh 1 the (de doi san pham). */
    public List<ProductCard> variantCardsForCategory(Integer categoryId) {
        return variants.findByProductCategoryIdAndIsActiveTrueAndStockQuantityGreaterThan(categoryId, 0)
                .stream().map(this::cardForVariant).collect(Collectors.toList());
    }

    public Optional<ProductVariant> findVariant(Integer variantId) {
        return variants.findById(variantId);
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

    /** Variant hien thi o trang chu: variant dang giam gia truoc, roi den gia thap nhat. */
    private ProductVariant firstVariant(Product p) {
        return p.getVariants().stream()
                .filter(v -> Boolean.TRUE.equals(v.getIsActive()))
                .sorted(Comparator.comparing((ProductVariant v) -> !v.onSale())
                        .thenComparing(ProductVariant::getPrice, Comparator.nullsLast(Comparator.naturalOrder())))
                .findFirst()
                .orElse(null);
    }

    private String imageFor(Product p, ProductVariant v) {
        return imageForVariant(v);
    }

    /** Anh cua 1 variant: URL that giu nguyen, example.com / rong thi doi sang anh local. */
    public String imageForVariant(ProductVariant v) {
        String url = v.getImgUrl();
        // example.com (DB mau) va images.unsplash.com (anh demo ban cu) deu khong phai
        // anh that cua shop, nen doi sang anh local cho 8 san pham cu; URL local
        // (/images/...) hoac URL that khac thi giu nguyen.
        if (url == null || url.isBlank() || url.contains("example.com") || url.contains("images.unsplash.com")) {
            Product p = v.getProduct();
            if (p != null && p.getMedia() != null && !p.getMedia().isEmpty()) {
                // Neu ProductMedia sau nay duoc cap nhat sang anh local that thi uu tien no.
                String media = p.getMedia().stream()
                        .filter(x -> "IMAGE".equals(x.getMediaType()))
                        .map(ProductMedia::getMediaUrl)
                        .filter(m -> m != null && !m.contains("example.com") && !m.contains("images.unsplash.com"))
                        .findFirst().orElse(null);
                if (media != null) {
                    return media;
                }
            }
            Integer productId = p != null ? p.getId() : null;
            return productId != null
                    ? LOCAL_IMAGES.getOrDefault(productId, "/images/products/makeup-remover/nuoc-tay-trang-cocoon-bi-dao-1.jpg")
                    : "/images/products/makeup-remover/nuoc-tay-trang-cocoon-bi-dao-1.jpg";
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
