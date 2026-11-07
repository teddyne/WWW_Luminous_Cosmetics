package com.luminous.service.admin;

import com.luminous.dto.ProductForm;
import com.luminous.dto.ProductRow;
import com.luminous.model.Category;
import com.luminous.model.enums.OrderStatus;
import com.luminous.model.Product;
import com.luminous.model.ProductMedia;
import com.luminous.model.ProductVariant;
import com.luminous.repository.category.CategoryRepository;
import com.luminous.repository.order.OrderDetailRepository;
import com.luminous.repository.product.ProductMediaRepository;
import com.luminous.repository.product.ProductRepository;
import com.luminous.repository.product.ProductVariantRepository;
import com.luminous.service.FileStorageService;
import com.luminous.service.ProductImage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class AdminProductService {

    public record SkinType(String code, String label) {
    }

    public static final List<SkinType> SKIN_TYPES = List.of(
            new SkinType("OILY", "Da dầu"),
            new SkinType("DRY", "Da khô"),
            new SkinType("COMBINATION", "Da hỗn hợp"),
            new SkinType("NORMAL", "Da thường"),
            new SkinType("SENSITIVE", "Da nhạy cảm"));

    private final ProductRepository products;
    private final CategoryRepository categories;
    private final ProductVariantRepository variants;
    private final ProductMediaRepository media;
    private final OrderDetailRepository orderDetails;
    private final FileStorageService files;

    public AdminProductService(ProductRepository products,
                               CategoryRepository categories,
                               ProductVariantRepository variants,
                               ProductMediaRepository media,
                               OrderDetailRepository orderDetails,
                               FileStorageService files) {
        this.products = products;
        this.categories = categories;
        this.variants = variants;
        this.media = media;
        this.orderDetails = orderDetails;
        this.files = files;
    }

    public Page<ProductRow> page(String q, Integer categoryId, Boolean active, String sort, int page, int size) {
        Sort sortOrder = switch (sort == null ? "newest" : sort) {
            case "oldest" -> Sort.by("createdAt").ascending();
            case "nameAsc" -> Sort.by("name").ascending();
            case "nameDesc" -> Sort.by("name").descending();
            default -> Sort.by("createdAt").descending();
        };
        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<Product> result = products.findAll(spec(q, categoryId, active), pageable);
        Map<Integer, Long> sold = soldMap();
        Map<Integer, List<ProductVariant>> variantsByProduct = variantsByProduct(result.getContent());
        return result.map(p -> toRow(p, sold.getOrDefault(p.getId(), 0L),
                variantsByProduct.getOrDefault(p.getId(), List.of())));
    }

    private Map<Integer, List<ProductVariant>> variantsByProduct(List<Product> pageProducts) {
        Map<Integer, List<ProductVariant>> grouped = new HashMap<>();
        if (pageProducts.isEmpty()) {
            return grouped;
        }
        List<Integer> ids = pageProducts.stream().map(Product::getId).toList();
        for (ProductVariant variant : variants.findByProductIdIn(ids)) {
            grouped.computeIfAbsent(variant.getProduct().getId(), key -> new ArrayList<>()).add(variant);
        }
        return grouped;
    }

    private Specification<Product> spec(String q, Integer categoryId, Boolean active) {
        return (root, query, cb) -> {
            List<Predicate> and = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                Join<Product, ProductVariant> variant = root.join("variants", JoinType.LEFT);
                query.distinct(true);
                and.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("brand")), like),
                        cb.like(cb.lower(variant.get("sku")), like)));
            }
            if (categoryId != null) {
                and.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (active != null) {
                and.add(cb.equal(root.get("isActive"), active));
            }
            return and.isEmpty() ? null : cb.and(and.toArray(Predicate[]::new));
        };
    }

    @Transactional
    public int[] bulkDelete(List<Integer> ids) {
        int deleted = 0;
        int skipped = 0;
        for (Integer id : ids) {
            try {
                delete(id);
                deleted++;
            } catch (IllegalArgumentException e) {
                skipped++;
            }
        }
        return new int[]{deleted, skipped};
    }

    @Transactional
    public int bulkSetActive(List<Integer> ids, boolean active) {
        int changed = 0;
        for (Integer id : ids) {
            Product product = products.findById(id).orElse(null);
            if (product == null || Boolean.valueOf(active).equals(product.getIsActive())) {
                continue;
            }
            product.setIsActive(active);
            products.save(product);
            changed++;
        }
        return changed;
    }

    public List<Category> categories() {
        return categories.findAllByOrderByNameAsc();
    }

    public ProductForm formFor(Integer id) {
        Product product = products.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));
        ProductVariant variant = ProductImage.firstVariant(product);
        if (variant == null) {
            variant = variants.findFirstByProductIdOrderByPriceAsc(id).orElse(null);
        }
        ProductForm form = new ProductForm();
        form.setId(product.getId());
        form.setName(product.getName());
        form.setBrand(product.getBrand());
        form.setCategoryId(product.getCategory().getId());
        form.setDescription(product.getDescription());
        form.setActive(product.getIsActive());
        if (variant != null) {
            form.setSku(variant.getSku());
            form.setPrice(variant.getPrice());
            form.setSalePrice(variant.getSalePrice());
            form.setStockQuantity(variant.getStockQuantity());
            form.setSize(variant.getSize());
            form.setShadeName(variant.getShadeName());
            form.setHexCode(variant.getHexCode());
            form.setIngredients(variant.getIngredients());
            form.setImageUrl(variant.getImgUrl());
            if (variant.getSuitableSkinType() != null && !variant.getSuitableSkinType().isBlank()) {
                form.setSuitableSkinTypes(List.of(variant.getSuitableSkinType().split(",")));
            }
        }
        return form;
    }

    @Transactional
    public Integer create(ProductForm form, MultipartFile image, MultipartFile[] gallery) {
        validatePrices(form);
        Category category = categories.findById(form.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Loại sản phẩm không tồn tại"));
        String sku = resolveSku(form.getSku(), form, null);
        Product product = new Product();
        product.setCategory(category);
        product.setName(form.getName().trim());
        product.setBrand(blankToNull(form.getBrand()));
        product.setDescription(blankToNull(form.getDescription()));
        product.setIsActive(Boolean.TRUE.equals(form.getActive()));
        product.setCreatedAt(LocalDateTime.now());
        products.save(product);
        ProductVariant variant = buildVariant(product, form, sku);
        String storedImage = files.storeProductImage(image, category.getName(), form.getName());
        if (storedImage != null) {
            variant.setImgUrl(storedImage);
        } else if (form.getImageUrl() != null && !form.getImageUrl().isBlank()) {
            variant.setImgUrl(form.getImageUrl().trim());
        }
        variants.save(variant);
        saveMedia(product, variant.getImgUrl(), gallery, form.getVideoUrl());
        return product.getId();
    }

    @Transactional
    public void update(Integer id, ProductForm form, MultipartFile image, MultipartFile[] gallery) {
        validatePrices(form);
        Product product = products.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));
        Category category = categories.findById(form.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Loại sản phẩm không tồn tại"));
        String sku = resolveSku(form.getSku(), form, id);
        product.setCategory(category);
        product.setName(form.getName().trim());
        product.setBrand(blankToNull(form.getBrand()));
        product.setDescription(blankToNull(form.getDescription()));
        product.setIsActive(Boolean.TRUE.equals(form.getActive()));
        products.save(product);
        ProductVariant variant = variants.findFirstByProductIdOrderByPriceAsc(id).orElseGet(() -> {
            ProductVariant created = new ProductVariant();
            created.setProduct(product);
            return created;
        });
        applyVariantFields(variant, form, sku);
        String storedImage = files.storeProductImage(image, category.getName(), form.getName());
        if (storedImage != null) {
            variant.setImgUrl(storedImage);
        } else if (form.getImageUrl() != null && !form.getImageUrl().isBlank()) {
            variant.setImgUrl(form.getImageUrl().trim());
        }
        variants.save(variant);
        appendGallery(product, gallery, form.getVideoUrl());
        if (storedImage != null) {
            replaceMainMedia(product, storedImage);
        }
    }

    @Transactional
    public void toggleActive(Integer id) {
        Product product = products.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));
        product.setIsActive(!Boolean.TRUE.equals(product.getIsActive()));
        products.save(product);
    }

    @Transactional
    public void delete(Integer id) {
        if (orderDetails.existsByVariant_Product_Id(id)) {
            throw new IllegalArgumentException("Sản phẩm đã có trong đơn hàng nên không thể xóa, hãy chuyển sang ngừng bán");
        }
        media.deleteByProductId(id);
        variants.deleteByProductId(id);
        products.deleteById(id);
    }

    private void validatePrices(ProductForm form) {
        if (form.getSalePrice() != null && form.getPrice() != null
                && form.getSalePrice().compareTo(form.getPrice()) > 0) {
            throw new IllegalArgumentException("Giá khuyến mãi không được cao hơn giá bán");
        }
    }

    private String resolveSku(String rawSku, ProductForm form, Integer productId) {
        String sku = rawSku == null ? "" : rawSku.trim().toUpperCase();
        if (sku.isBlank()) {
            String brand = form.getBrand() == null || form.getBrand().isBlank() ? "LUM" : form.getBrand();
            String prefix = FileStorageService.slug(brand).toUpperCase().replace("-", "");
            prefix = prefix.length() > 3 ? prefix.substring(0, 3) : prefix;
            String namePart = FileStorageService.slug(form.getName()).replace("-", "").toUpperCase();
            namePart = namePart.length() > 6 ? namePart.substring(0, 6) : namePart;
            sku = prefix + "-" + namePart + "-" + (System.currentTimeMillis() % 10000);
        }
        final String checkedSku = sku;
        boolean duplicate = variants.existsBySkuIgnoreCase(sku)
                && (productId == null || variants.findFirstByProductIdOrderByPriceAsc(productId)
                        .map(v -> !v.getSku().equalsIgnoreCase(checkedSku)).orElse(true));
        if (duplicate) {
            throw new IllegalArgumentException("Mã sản phẩm đã tồn tại");
        }
        return sku;
    }

    private ProductVariant buildVariant(Product product, ProductForm form, String sku) {
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        applyVariantFields(variant, form, sku);
        return variant;
    }

    private void applyVariantFields(ProductVariant variant, ProductForm form, String sku) {
        variant.setSku(sku);
        variant.setPrice(form.getPrice());
        variant.setSalePrice(form.getSalePrice());
        variant.setStockQuantity(form.getStockQuantity() == null ? 0 : form.getStockQuantity());
        variant.setSize(blankToNull(form.getSize()));
        variant.setShadeName(blankToNull(form.getShadeName()));
        variant.setHexCode(blankToNull(form.getHexCode()));
        variant.setIngredients(blankToNull(form.getIngredients()));
        variant.setSuitableSkinType(form.getSuitableSkinTypes() == null || form.getSuitableSkinTypes().isEmpty()
                ? null : String.join(",", form.getSuitableSkinTypes()));
        variant.setIsActive(true);
    }

    private void saveMedia(Product product, String mainImage, MultipartFile[] gallery, String videoUrl) {
        int order = 1;
        if (mainImage != null && !mainImage.isBlank() && !mainImage.contains("example.com")) {
            media.save(imageMedia(product, mainImage, order++));
        }
        order = appendGalleryFiles(product, gallery, order);
        if (videoUrl != null && !videoUrl.isBlank()) {
            ProductMedia video = new ProductMedia();
            video.setProduct(product);
            video.setMediaUrl(videoUrl.trim());
            video.setMediaType("VIDEO");
            video.setDisplayOrder(order);
            media.save(video);
        }
    }

    private void appendGallery(Product product, MultipartFile[] gallery, String videoUrl) {
        List<ProductMedia> existing = media.findByProductIdOrderByDisplayOrderAsc(product.getId());
        int order = existing.stream().mapToInt(ProductMedia::getDisplayOrder).max().orElse(0) + 1;
        order = appendGalleryFiles(product, gallery, order);
        if (videoUrl != null && !videoUrl.isBlank()) {
            boolean exists = existing.stream()
                    .anyMatch(m -> "VIDEO".equals(m.getMediaType()) && videoUrl.trim().equals(m.getMediaUrl()));
            if (!exists) {
                ProductMedia video = new ProductMedia();
                video.setProduct(product);
                video.setMediaUrl(videoUrl.trim());
                video.setMediaType("VIDEO");
                video.setDisplayOrder(order);
                media.save(video);
            }
        }
    }

    private int appendGalleryFiles(Product product, MultipartFile[] gallery, int order) {
        if (gallery == null) {
            return order;
        }
        for (MultipartFile file : gallery) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String categoryName = product.getCategory() == null ? null : product.getCategory().getName();
            String stored = files.storeProductImage(file, categoryName, product.getName());
            if (stored != null) {
                media.save(imageMedia(product, stored, order++));
            }
        }
        return order;
    }

    private void replaceMainMedia(Product product, String storedImage) {
        List<ProductMedia> existing = media.findByProductIdOrderByDisplayOrderAsc(product.getId());
        ProductMedia main = existing.stream()
                .filter(m -> "IMAGE".equals(m.getMediaType()))
                .findFirst().orElse(null);
        if (main == null) {
            media.save(imageMedia(product, storedImage, 1));
        } else {
            main.setMediaUrl(storedImage);
            main.setThumbnailUrl(storedImage);
            media.save(main);
        }
    }

    private ProductMedia imageMedia(Product product, String url, int order) {
        ProductMedia item = new ProductMedia();
        item.setProduct(product);
        item.setMediaUrl(url);
        item.setThumbnailUrl(url);
        item.setMediaType("IMAGE");
        item.setDisplayOrder(order);
        item.setAltText(product.getName());
        return item;
    }

    private Map<Integer, Long> soldMap() {
        Map<Integer, Long> sold = new HashMap<>();
        for (Object[] row : orderDetails.soldByProduct(OrderStatus.CANCELLED)) {
            sold.put((Integer) row[0], ((Number) row[1]).longValue());
        }
        return sold;
    }

    private ProductRow toRow(Product product, long soldCount, List<ProductVariant> allVariants) {
        List<ProductVariant> active = allVariants.stream()
                .filter(v -> Boolean.TRUE.equals(v.getIsActive()))
                .toList();
        BigDecimal min = active.stream().map(ProductVariant::getPrice).min(BigDecimal::compareTo).orElse(null);
        BigDecimal max = active.stream().map(ProductVariant::getPrice).max(BigDecimal::compareTo).orElse(null);
        int stock = active.stream().mapToInt(v -> v.getStockQuantity() == null ? 0 : v.getStockQuantity()).sum();
        Integer firstVariantId = active.stream()
                .min(Comparator.comparing(ProductVariant::getPrice,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(ProductVariant::getId).orElse(null);
        return new ProductRow(
                product.getId(),
                product.getName(),
                product.getBrand(),
                product.getCategory() != null ? product.getCategory().getName() : "",
                ProductImage.imageFor(product, allVariants),
                min, max, stock, soldCount,
                Boolean.TRUE.equals(product.getIsActive()),
                product.getCreatedAt(),
                allVariants.size(),
                firstVariantId);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
