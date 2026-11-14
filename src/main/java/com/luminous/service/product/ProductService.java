package com.luminous.service.product;

import com.luminous.dto.ProductCard;
import com.luminous.dto.ReviewView;
import com.luminous.model.Product;
import com.luminous.model.ProductMedia;
import com.luminous.model.ProductVariant;
import com.luminous.model.Review;
import com.luminous.repository.UserRepository;
import com.luminous.repository.order.OrderDetailRepository;
import com.luminous.repository.product.ProductAttributeRepository;
import com.luminous.repository.product.ProductRepository;
import com.luminous.repository.product.ProductVariantRepository;
import com.luminous.repository.review.ReviewMediaRepository;
import com.luminous.repository.review.ReviewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
    private final ReviewMediaRepository reviewMedia;
    private final ProductVariantRepository variants;
    private final ProductAttributeRepository productAttributes;
    private final UserRepository users;

    /** Anh cho san chung khi san pham chua co anh nao (khong do ve anh san pham khac). */
    private static final String NO_IMAGE = "/images/no-image.svg";

    /** Tom tat danh gia: diem trung binh, tong so, so luong theo tung muc sao. */
    public record ReviewSummary(double average, long total, Map<Integer, Long> byStar) {
        /** So sao hien thi theo diem trung binh (lam tron: >= 4.5 -> 5 sao...). */
        public int starCount() {
            return average >= 4.5 ? 5 : average >= 3.5 ? 4 : average >= 2.5 ? 3
                    : average >= 1.5 ? 2 : average >= 0.5 ? 1 : 0;
        }

        /** Chuoi sao hien thi theo diem trung binh. */
        public String stars() {
            return "★".repeat(starCount());
        }

        /** So danh gia cua 1 muc sao (dung o template thay cho byStar[star]:
         *  SpEL khong hieu chi so bien tren Map<Integer, Long> nen byStar[star]
         *  bi doc thanh chuoi "star" -> NumberFormatException). */
        public long countFor(int star) {
            return byStar == null ? 0L : byStar.getOrDefault(star, 0L);
        }
    }

    public ProductService(ProductRepository products,
                          OrderDetailRepository orderDetails,
                          ReviewRepository reviews,
                          ReviewMediaRepository reviewMedia,
                          ProductVariantRepository variants,
                          ProductAttributeRepository productAttributes,
                          UserRepository users) {
        this.products = products;
        this.orderDetails = orderDetails;
        this.reviews = reviews;
        this.reviewMedia = reviewMedia;
        this.variants = variants;
        this.productAttributes = productAttributes;
        this.users = users;
    }

    /** Trang danh gia cua 1 san pham, loc theo sao (rating null = tat ca). */
    public Page<ReviewView> reviewPage(Integer productId, Integer rating, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(0, page), size <= 0 ? 6 : size);
        Page<Review> result = (rating != null && rating >= 1 && rating <= 5)
                ? reviews.findVisibleByProductAndRating(productId, rating.byteValue(), pageable)
                : reviews.findVisibleByProduct(productId, pageable);
        List<Integer> reviewIds = result.getContent().stream().map(Review::getId).toList();
        Map<Integer, List<com.luminous.model.ReviewMedia>> mediaByReview =
                reviewIds.isEmpty() ? Map.of()
                        : reviewMedia.findByReviewIdIn(reviewIds).stream()
                                .collect(Collectors.groupingBy(
                                        m -> m.getReview().getId()));
        List<ReviewView> views = result.getContent().stream().map(r -> {
            var media = mediaByReview.getOrDefault(r.getId(), List.of());
            return new ReviewView(
                    r.getRating() == null ? 0 : r.getRating().intValue(),
                    r.getComment(), r.getCreatedAt(), reviewerName(r),
                    media.stream().filter(m -> "IMAGE".equals(m.getMediaType()))
                            .map(com.luminous.model.ReviewMedia::getMediaUrl).toList(),
                    media.stream().filter(m -> "VIDEO".equals(m.getMediaType()))
                            .map(com.luminous.model.ReviewMedia::getMediaUrl).toList());
        }).toList();
        return new PageImpl<>(views, pageable, result.getTotalElements());
    }

    public ReviewSummary reviewSummary(Integer productId) {
        Double avg = reviews.averageRating(productId);
        Map<Integer, Long> byStar = new HashMap<>();
        for (int star = 1; star <= 5; star++) {
            byStar.put(star, 0L);
        }
        for (Object[] row : reviews.countByRating(productId)) {
            byStar.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
        }
        return new ReviewSummary(avg != null ? avg : 0, reviews.reviewCount(productId), byStar);
    }

    /** Ten nguoi danh gia: an danh thi an ten, neu khong thi che 1 phan (c*****a). */
    private String reviewerName(Review review) {
        if (Boolean.TRUE.equals(review.getIsAnonymous())) {
            return "Khách hàng ẩn danh";
        }
        String name = users.findById(review.getUserId())
                .map(u -> u.getFullName()).orElse(null);
        if (name == null || name.isBlank()) {
            return "Khách hàng";
        }
        String trimmed = name.trim();
        if (trimmed.length() < 2) {
            return trimmed;
        }
        return trimmed.charAt(0) + "*****" + trimmed.charAt(trimmed.length() - 1);
    }

    /** URL video dau tien cua san pham (ProductMedia VIDEO), null neu khong co. */
    public String productVideoUrl(ProductCard card) {
        if (card == null || card.getProduct() == null) {
            return null;
        }
        return card.getProduct().getMedia().stream()
                .filter(m -> "VIDEO".equals(m.getMediaType()))
                .map(ProductMedia::getMediaUrl)
                .filter(url -> url != null && !url.isBlank())
                .findFirst().orElse(null);
    }

    public List<ProductCard> deals(int limit) {
        return inStockCards(products.findDeals(), limit);
    }

    public List<ProductCard> newest(int limit) {
        return inStockCards(products.findByIsActiveTrueOrderByCreatedAtDesc(), limit);
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
        return inStockCards(rest, limit);
    }

    public List<ProductCard> bestSellers(int limit) {
        List<Product> best = orderDetails.findBestSellers();
        // Neu DB chua co don hang nao, best sellers se rong -> fallback ve san pham moi nhat
        // de trang chu khong bi khoi trong (van la du lieu that tu DB, khong phai so gia).
        if (best.isEmpty()) {
            return newest(limit);
        }
        return inStockCards(best.stream().filter(p -> Boolean.TRUE.equals(p.getIsActive())).toList(), limit);
    }

    public List<ProductCard> allActiveCards() {
        return inStockCards(products.findByIsActiveTrueOrderByCreatedAtDesc(), Integer.MAX_VALUE);
    }

    /**
     * Tim kiem + loc + sap xep cho trang San pham (/products).
     * Tham so nao null/rong thi bo qua (khong loc theo tieu chi do).
     * Loc gia: chi can 1 variant dang ban co gia trong khoang la tinh
     * (san pham co nhieu dung tich nen khong the lay 1 gia dai dien).
     * onSaleOnly = true: chi lay san pham dang giam gia (nut "Xem them"
     * o phan "Dai tiec uu dai" tren trang chu dan sang day).
     */
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

    public Page<ProductCard> searchPage(String q, String brand, List<Integer> categoryIds,
                                        BigDecimal minPrice, BigDecimal maxPrice,
                                        Double minRating, String sort, boolean onSaleOnly,
                                        int page, int size) {
        int safeSize = size <= 0 ? 12 : size;
        List<ProductCard> all = search(q, brand, categoryIds, minPrice, maxPrice, minRating, sort, onSaleOnly);
        int totalPages = (int) Math.ceil((double) all.size() / safeSize);
        int safePage = Math.max(0, Math.min(page, Math.max(0, totalPages - 1)));
        int from = safePage * safeSize;
        int to = Math.min(from + safeSize, all.size());
        List<ProductCard> content = from >= all.size() ? List.of() : all.subList(from, to);
        return new PageImpl<>(content, PageRequest.of(safePage, safeSize), all.size());
    }

    private boolean hasVariantInPriceRange(ProductCard card, BigDecimal min, BigDecimal max) {
        return card.getProduct().getVariants().stream()
                .filter(v -> Boolean.TRUE.equals(v.getIsActive()))
                .map(ProductVariant::displayPrice)
                .filter(Objects::nonNull)
                .anyMatch(p -> (min == null || p.compareTo(min) >= 0)
                        && (max == null || p.compareTo(max) <= 0));
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

    /** Du lieu day du cho trang chi tiet, chi cho phep san pham dang ban. */
    public Optional<ProductCard> detailCard(Integer productId) {
        return products.findById(productId)
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                .map(p -> toCards(List.of(p), 1))
                .filter(list -> !list.isEmpty())
                .map(list -> list.get(0));
    }

    /** Cac bien the dang ban cua san pham, dung cho nut chon dung tich/mau. */
    public List<ProductCard> activeVariantCards(Integer productId) {
        return variants.findByProductIdAndIsActiveTrue(productId).stream()
                .map(this::cardForVariant)
                .toList();
    }

    /** San pham lien quan: cung danh muc, con hoat dong, con hang, khong lap san pham hien tai. */
    public List<ProductCard> relatedCards(Integer categoryId, Integer excludeProductId, int limit) {
        return products.findByIsActiveTrueAndCategoryIdOrderByCreatedAtDesc(categoryId).stream()
                .filter(p -> !Objects.equals(p.getId(), excludeProductId))
                .filter(this::hasStock)
                .filter(p -> firstVariant(p) != null)
                .map(p -> toCards(List.of(p), 1).get(0))
                .limit(limit)
                .toList();
    }

    /** Danh sach anh gallery, uu tien anh variant hien tai va anh trong ProductMedia. */
    public List<String> galleryImages(ProductCard card) {
        LinkedHashSet<String> urls = new LinkedHashSet<>();
        urls.add(card.getImageUrl());
        Product p = card.getProduct();
        p.getMedia().stream()
                .filter(m -> "IMAGE".equals(m.getMediaType()))
                .map(m -> m.getThumbnailUrl() != null ? m.getThumbnailUrl() : m.getMediaUrl())
                .filter(Objects::nonNull)
                .filter(url -> !url.contains("example.com") && !url.contains("images.unsplash.com"))
                .forEach(urls::add);
        return new ArrayList<>(urls);
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

    /** San pham con hang: co it nhat 1 bien the dang ban con ton kho > 0.
     *  San pham het hang khong hien o trang chu / danh sach / lien quan
     *  (trang chi tiet van mo duoc, hien "Tam het hang" va khoa nut them). */
    private boolean hasStock(Product p) {
        return p.getVariants().stream()
                .anyMatch(v -> Boolean.TRUE.equals(v.getIsActive())
                        && v.getStockQuantity() != null && v.getStockQuantity() > 0);
    }

    /** The san pham chi tu san pham con hang (dung cho trang chu / tim kiem / lien quan). */
    private List<ProductCard> inStockCards(List<Product> list, int limit) {
        return toCards(list.stream().filter(this::hasStock).toList(), limit);
    }

    /** Thong so san pham (bang o trang chi tiet), theo thu tu admin nhap. */
    public List<com.luminous.model.ProductAttribute> productAttributes(Integer productId) {
        return productAttributes.findByProductIdOrderByDisplayOrderAsc(productId);
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
        } else if (p != null && !p.getMedia().isEmpty()) {
            ProductMedia m = p.getMedia().stream()
                    .filter(x -> "IMAGE".equals(x.getMediaType()))
                    .findFirst().orElse(p.getMedia().get(0));
            url = m.getThumbnailUrl() != null ? m.getThumbnailUrl() : m.getMediaUrl();
        }
        // Du lieu cu con sot URL example.com / anh demo unsplash khong tai duoc,
        // hoac san pham chua co anh nao -> anh cho san chung, khong do ve anh san pham khac.
        if (url == null || url.contains("example.com") || url.contains("images.unsplash.com")) {
            return NO_IMAGE;
        }
        return url;
    }

    /** Anh cua 1 variant (dung o chu trinh da, chi tiet, doi san pham). */
    public String imageForVariant(ProductVariant v) {
        if (v == null) {
            return NO_IMAGE;
        }
        return imageFor(v.getProduct(), v);
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
