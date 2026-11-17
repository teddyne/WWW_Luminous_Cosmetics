package com.luminous.service.product;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luminous.dto.ProductCard;
import com.luminous.dto.CachedProduct;
import com.luminous.model.Category;
import com.luminous.model.Product;
import com.luminous.model.ProductMedia;
import com.luminous.model.ProductVariant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;



@Service
public class ProductCacheService {

    private static final Logger log = LoggerFactory.getLogger(ProductCacheService.class);

    public static final String SEARCH_PREFIX = "luminous:products:search:";
    public static final String PRODUCT_PREFIX = "luminous:product:";
    public static final String HOME_PREFIX = "luminous:home:";

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final long ttlSeconds;
    private final boolean enabled;

    /** Chi log canh bao loi Redis 1 lan de khong spam log moi request. */
    private final AtomicBoolean warned = new AtomicBoolean(false);

    public ProductCacheService(StringRedisTemplate redis,
                               @Qualifier("productCacheObjectMapper") ObjectMapper mapper,
                               @Value("${app.cache.product-ttl-seconds:300}") long ttlSeconds,
                               @Value("${app.cache.enabled:true}") boolean enabled) {
        this.redis = redis;
        this.mapper = mapper;
        this.ttlSeconds = ttlSeconds;
        this.enabled = enabled;
    }

    // ---------- Khoa cache ----------

    /** Khoa trang tim kiem: phai chua DU tham so loc/sap xep/phan trang. */
    public String searchKey(String q, String brand, List<Integer> categoryIds,
                            BigDecimal minPrice, BigDecimal maxPrice,
                            Double minRating, String sort, boolean onSaleOnly,
                            int page, int size) {
        String raw = "q=" + s(q) + "|brand=" + s(brand) + "|cat=" + s(categoryIds)
                + "|min=" + s(minPrice) + "|max=" + s(maxPrice) + "|rating=" + s(minRating)
                + "|sort=" + s(sort) + "|sale=" + onSaleOnly + "|page=" + page + "|size=" + size;
        return SEARCH_PREFIX + sha256(raw);
    }

    public String productKey(Integer productId) {
        return PRODUCT_PREFIX + productId;
    }

    public String homeKey(String kind, int limit) {
        return HOME_PREFIX + kind + ":" + limit;
    }

    // ---------- Trang tim kiem ----------

    public record CachedPage(long total, List<CachedProduct> items) {
    }

    public CachedPage getSearchPage(String key) {
        String json = get(key);
        if (json == null) {
            return null;
        }
        try {
            return mapper.readValue(json, CachedPage.class);
        } catch (Exception e) {
            log.warn("Khong doc duoc cache tim kiem {}: {}", key, e.getMessage());
            return null;
        }
    }

    public void putSearchPage(String key, long total, List<ProductCard> cards) {
        put(key, toJson(new CachedPage(total, cards.stream().map(this::fromCard).toList())));
    }

    // ---------- Chi tiet 1 san pham ----------

    public ProductCard getProduct(Integer productId) {
        String json = get(productKey(productId));
        if (json == null) {
            return null;
        }
        try {
            return toCard(mapper.readValue(json, CachedProduct.class));
        } catch (Exception e) {
            log.warn("Khong doc duoc cache san pham {}: {}", productId, e.getMessage());
            return null;
        }
    }

    public void putProduct(ProductCard card) {
        if (card == null || card.getProduct() == null || card.getProduct().getId() == null) {
            return;
        }
        put(productKey(card.getProduct().getId()), toJson(fromCard(card)));
    }

    // ---------- Trang chu ----------

    public List<ProductCard> getHomeList(String kind, int limit) {
        String json = get(homeKey(kind, limit));
        if (json == null) {
            return null;
        }
        try {
            List<CachedProduct> items = mapper.readValue(json,
                    new TypeReference<List<CachedProduct>>() {
                    });
            return items.stream().map(this::toCard).toList();
        } catch (Exception e) {
            log.warn("Khong doc duoc cache trang chu {}: {}", kind, e.getMessage());
            return null;
        }
    }

    public void putHomeList(String kind, int limit, List<ProductCard> cards) {
        put(homeKey(kind, limit), toJson(cards.stream().map(this::fromCard).toList()));
    }

    // ---------- Xoa cache ----------

    /** Xoa toan bo cache san pham (admin them/sua/xoa san pham, dat hang xong). */
    public void evictAll() {
        evictPattern(SEARCH_PREFIX + "*");
        evictPattern(PRODUCT_PREFIX + "*");
        evictPattern(HOME_PREFIX + "*");
    }

    // ---------- Chuyen doi Card <-> CachedProduct ----------

    public CachedProduct fromCard(ProductCard card) {
        Product p = card.getProduct();
        ProductVariant v = card.getVariant();
        List<String> mediaUrls = p.getMedia() == null ? List.of()
                : p.getMedia().stream()
                .filter(m -> "IMAGE".equals(m.getMediaType()))
                .map(m -> m.getThumbnailUrl() != null ? m.getThumbnailUrl() : m.getMediaUrl())
                .filter(Objects::nonNull).toList();
        List<BigDecimal> variantPrices = p.getVariants() == null ? List.of()
                : p.getVariants().stream()
                .filter(x -> Boolean.TRUE.equals(x.getIsActive()))
                .map(ProductVariant::displayPrice)
                .filter(Objects::nonNull).toList();
        return new CachedProduct(
                p.getId(),
                v == null ? null : v.getId(),
                p.getName(),
                p.getBrand(),
                p.getCategory() == null ? null : p.getCategory().getId(),
                p.getCategory() == null ? null : p.getCategory().getName(),
                v == null ? null : v.getPrice(),
                v == null ? null : v.getSalePrice(),
                v == null ? null : v.getStockQuantity(),
                card.getImageUrl(),
                card.getRating(),
                card.getReviewCount(),
                card.getSoldCount(),
                p.getDescription(),
                p.getUsageInstructions(),
                mediaUrls,
                variantPrices,
                p.getCreatedAt() == null ? null : p.getCreatedAt().toString(),
                v == null ? null : v.getSize(),
                v == null ? null : v.getShadeName());
    }

    /**
     * Dung lai ProductCard tu cache: Product/Variant tach roi (detached),
     * du truong de cac ham name()/displayPrice()/priceRangeText()/onSale()
     * cua ProductCard chay nhu binh thuong. Khoang gia lay tu variantPrices.
     */
    public ProductCard toCard(CachedProduct c) {
        Category category = new Category();
        category.setId(c.categoryId());
        category.setName(c.categoryName());

        Product product = new Product();
        product.setId(c.productId());
        product.setName(c.name());
        product.setBrand(c.brand());
        product.setCategory(category);
        product.setDescription(c.description());
        product.setUsageInstructions(c.usageInstructions());
        product.setIsActive(true);
        product.setCreatedAt(c.createdAt() == null ? null : LocalDateTime.parse(c.createdAt()));

        ProductVariant variant = new ProductVariant();
        variant.setId(c.variantId());
        variant.setProduct(product);
        variant.setPrice(c.price());
        variant.setSalePrice(c.salePrice());
        variant.setStockQuantity(c.stockQuantity());
        variant.setSize(c.size());
        variant.setShadeName(c.shadeName());
        variant.setIsActive(true);

        // Cac bien the khac chi de tinh khoang gia: gia hien thi giong nhau
        // thi chi can 1 dong; dong dau tien luon la bien the that.
        List<ProductVariant> allVariants = new ArrayList<>();
        allVariants.add(variant);
        for (BigDecimal displayPrice : c.variantPrices()) {
            boolean already = allVariants.stream()
                    .anyMatch(x -> displayPrice != null && displayPrice.equals(x.displayPrice()));
            if (!already) {
                ProductVariant stub = new ProductVariant();
                stub.setProduct(product);
                stub.setPrice(displayPrice);
                stub.setIsActive(true);
                allVariants.add(stub);
            }
        }
        product.setVariants(allVariants);

        List<ProductMedia> media = new ArrayList<>();
        int order = 1;
        for (String url : c.mediaUrls()) {
            ProductMedia m = new ProductMedia();
            m.setProduct(product);
            m.setMediaUrl(url);
            m.setThumbnailUrl(url);
            m.setMediaType("IMAGE");
            m.setDisplayOrder(order++);
            media.add(m);
        }
        product.setMedia(media);

        return new ProductCard(product, variant, c.imageUrl(), c.rating(),
                c.reviewCount(), c.soldCount());
    }

    // ---------- Tac vu Redis (luon bat loi) ----------

    private String get(String key) {
        if (!enabled || key == null) {
            return null;
        }
        try {
            ValueOperations<String, String> ops = redis.opsForValue();
            String value = ops.get(key);
            if (value != null) {
                warned.set(false);
            }
            return value;
        } catch (Exception e) {
            warnOnce("Khong doc duoc Redis (dung MariaDB thay the)", e);
            return null;
        }
    }

    private void put(String key, String json) {
        if (!enabled || key == null || json == null) {
            return;
        }
        try {
            redis.opsForValue().set(key, json, ttlSeconds, TimeUnit.SECONDS);
            warned.set(false);
        } catch (Exception e) {
            warnOnce("Khong ghi duoc Redis (bo qua cache)", e);
        }
    }

    private void evictPattern(String pattern) {
        if (!enabled) {
            return;
        }
        try {
            Set<String> keys = redis.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redis.delete(keys);
            }
            warned.set(false);
        } catch (Exception e) {
            warnOnce("Khong xoa duoc cache Redis theo mau " + pattern, e);
        }
    }

    private String toJson(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("Khong serialize duoc cache: {}", e.getMessage());
            return null;
        }
    }

    private void warnOnce(String message, Exception e) {
        if (warned.compareAndSet(false, true)) {
            log.warn("{}: {}", message, e.getMessage());
        }
    }

    private static String s(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).collect(Collectors.joining(","));
        }
        return String.valueOf(value);
    }

    private static String sha256(String raw) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return Integer.toHexString(raw.hashCode());
        }
    }
}
