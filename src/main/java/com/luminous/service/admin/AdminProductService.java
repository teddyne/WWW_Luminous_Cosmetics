package com.luminous.service.admin;

import com.luminous.dto.ProductForm;
import com.luminous.dto.ProductRow;
import com.luminous.model.Category;
import com.luminous.model.enums.OrderStatus;
import com.luminous.model.Product;
import com.luminous.model.ProductAttribute;
import com.luminous.model.ProductMedia;
import com.luminous.model.ProductVariant;
import com.luminous.repository.category.CategoryRepository;
import com.luminous.repository.order.OrderDetailRepository;
import com.luminous.repository.product.ProductAttributeRepository;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final ProductAttributeRepository attributes;
    private final OrderDetailRepository orderDetails;
    private final FileStorageService files;

    public AdminProductService(ProductRepository products,
                               CategoryRepository categories,
                               ProductVariantRepository variants,
                               ProductMediaRepository media,
                               ProductAttributeRepository attributes,
                               OrderDetailRepository orderDetails,
                               FileStorageService files) {
        this.products = products;
        this.categories = categories;
        this.variants = variants;
        this.media = media;
        this.attributes = attributes;
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
        ProductForm form = new ProductForm();
        form.setId(product.getId());
        form.setName(product.getName());
        form.setBrand(product.getBrand());
        form.setCategoryId(product.getCategory().getId());
        form.setDescription(product.getDescription());
        form.setUsageInstructions(product.getUsageInstructions());
        form.setActive(product.getIsActive());
        List<ProductVariant> all = variants.findByProductIdAndIsActiveTrue(id).stream()
                .sorted(Comparator.comparing(ProductVariant::getPrice,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        List<ProductForm.VariantForm> rows = new ArrayList<>();
        for (ProductVariant v : all) {
            ProductForm.VariantForm row = new ProductForm.VariantForm();
            row.setId(v.getId());
            row.setSku(v.getSku());
            row.setSize(v.getSize());
            row.setShadeName(v.getShadeName());
            row.setHexCode(v.getHexCode());
            row.setPrice(v.getPrice());
            row.setSalePrice(v.getSalePrice());
            row.setStockQuantity(v.getStockQuantity());
            rows.add(row);
        }
        if (rows.isEmpty()) {
            rows.add(new ProductForm.VariantForm());
        }
        form.setVariants(rows);
        List<ProductForm.AttributeForm> attrs = attributes.findByProductIdOrderByDisplayOrderAsc(id).stream()
                .map(a -> new ProductForm.AttributeForm(a.getName(), a.getValue()))
                .collect(Collectors.toList());
        if (attrs.isEmpty()) {
            attrs.add(new ProductForm.AttributeForm("", ""));
        }
        form.setAttributes(attrs);
        // Thanh phan / da phu hop / anh dai dien lay tu bien the dau tien (gia thap nhat).
        ProductVariant first = all.isEmpty() ? null : all.get(0);
        if (first != null) {
            form.setIngredients(first.getIngredients());
            form.setImageUrl(first.getImgUrl());
            if (first.getSuitableSkinType() != null && !first.getSuitableSkinType().isBlank()) {
                form.setSuitableSkinTypes(List.of(first.getSuitableSkinType().split(",")));
            }
        }
        return form;
    }

    @Transactional
    public Integer create(ProductForm form, MultipartFile image, MultipartFile[] images,
                          MultipartFile video) {
        validateVariants(form);
        Category category = categories.findById(form.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Loại sản phẩm không tồn tại"));
        Product product = new Product();
        applyProductFields(product, form, category);
        product.setCreatedAt(LocalDateTime.now());
        products.save(product);
        saveVariants(product, form);
        saveAttributes(product, form);
        // Anh chinh + nhieu anh phu + video: luu theo cau truc
        // static/products/{categoryId}-{slug-loai}/{productId}/images|videos/...
        String mainImage = saveNewMedia(product, category, image, images, video, form.getVideoUrl());
        ProductVariant first = variants.findFirstByProductIdOrderByPriceAsc(product.getId()).orElse(null);
        if (first != null) {
            if (mainImage != null) {
                first.setImgUrl(mainImage);
                variants.save(first);
            } else if (form.getImageUrl() != null && !form.getImageUrl().isBlank()) {
                first.setImgUrl(form.getImageUrl().trim());
                variants.save(first);
                media.save(imageMedia(product, form.getImageUrl().trim(), 1));
            }
        }
        return product.getId();
    }

    @Transactional
    public void update(Integer id, ProductForm form, MultipartFile image, MultipartFile[] images,
                       MultipartFile video) {
        validateVariants(form);
        Product product = products.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));
        Category category = categories.findById(form.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Loại sản phẩm không tồn tại"));
        applyProductFields(product, form, category);
        products.save(product);
        syncVariants(product, form);
        saveAttributes(product, form);
        String storedImage = appendMedia(product, category, image, images, video, form.getVideoUrl());
        ProductVariant first = variants.findFirstByProductIdOrderByPriceAsc(id).orElse(null);
        if (first != null) {
            if (storedImage != null) {
                first.setImgUrl(storedImage);
            } else if (form.getImageUrl() != null && !form.getImageUrl().isBlank()) {
                first.setImgUrl(form.getImageUrl().trim());
            }
            variants.save(first);
        }
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
        attributes.deleteByProductId(id);
        variants.deleteByProductId(id);
        products.deleteById(id);
    }

    private void applyProductFields(Product product, ProductForm form, Category category) {
        product.setCategory(category);
        product.setName(form.getName().trim());
        product.setBrand(blankToNull(form.getBrand()));
        product.setDescription(blankToNull(form.getDescription()));
        product.setUsageInstructions(blankToNull(form.getUsageInstructions()));
        product.setIsActive(Boolean.TRUE.equals(form.getActive()));
    }

    /** Kiem tra cac dong bien the: it nhat 1 dong, gia KM khong vuot gia ban, SKU khong trung nhau. */
    private void validateVariants(ProductForm form) {
        List<ProductForm.VariantForm> rows = form.getVariants() == null ? List.of() : form.getVariants();
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Cần ít nhất một biến thể");
        }
        Set<String> seen = new HashSet<>();
        for (ProductForm.VariantForm row : rows) {
            if (row.getSalePrice() != null && row.getPrice() != null
                    && row.getSalePrice().compareTo(row.getPrice()) > 0) {
                throw new IllegalArgumentException("Giá khuyến mãi không được cao hơn giá bán");
            }
            String sku = row.getSku() == null ? "" : row.getSku().trim().toLowerCase();
            if (!sku.isBlank() && !seen.add(sku)) {
                throw new IllegalArgumentException("Mã SKU " + row.getSku().trim().toUpperCase()
                        + " bị trùng trong các biến thể");
            }
        }
    }

    private void saveVariants(Product product, ProductForm form) {
        List<ProductForm.VariantForm> rows = form.getVariants();
        for (int i = 0; i < rows.size(); i++) {
            ProductForm.VariantForm row = rows.get(i);
            String sku = resolveVariantSku(row.getSku(), form, null, i);
            ProductVariant variant = new ProductVariant();
            variant.setProduct(product);
            applyVariantFields(variant, row, sku, form);
            variants.save(variant);
        }
    }

    /**
     * Dong bo bien the khi sua: cap nhat dong co id, them dong moi (id null),
     * dong bi xoa khoi form thi xoa han neu chua co don hang, neu khong thi ngung ban.
     */
    private void syncVariants(Product product, ProductForm form) {
        List<ProductForm.VariantForm> rows = form.getVariants();
        List<ProductVariant> existing = variants.findByProductIdOrderByPriceAsc(product.getId());
        Map<Integer, ProductVariant> byId = existing.stream()
                .filter(v -> v.getId() != null)
                .collect(Collectors.toMap(ProductVariant::getId, v -> v));
        Set<Integer> keptIds = new HashSet<>();
        for (int i = 0; i < rows.size(); i++) {
            ProductForm.VariantForm row = rows.get(i);
            String sku = resolveVariantSku(row.getSku(), form, row.getId(), i);
            ProductVariant variant = row.getId() != null ? byId.get(row.getId()) : null;
            if (variant == null) {
                variant = new ProductVariant();
                variant.setProduct(product);
            }
            applyVariantFields(variant, row, sku, form);
            variants.save(variant);
            keptIds.add(variant.getId());
        }
        for (ProductVariant old : existing) {
            if (old.getId() != null && !keptIds.contains(old.getId())) {
                if (orderDetails.existsByVariantId(old.getId())) {
                    old.setIsActive(false);
                    variants.save(old);
                } else {
                    variants.delete(old);
                }
            }
        }
    }

    private void applyVariantFields(ProductVariant variant, ProductForm.VariantForm row,
                                    String sku, ProductForm form) {
        variant.setSku(sku);
        variant.setPrice(row.getPrice());
        variant.setSalePrice(row.getSalePrice());
        variant.setStockQuantity(row.getStockQuantity() == null ? 0 : row.getStockQuantity());
        variant.setSize(blankToNull(row.getSize()));
        variant.setShadeName(blankToNull(row.getShadeName()));
        variant.setHexCode(blankToNull(row.getHexCode()));
        // Thanh phan + da phu hop nhap chung 1 o o form, ap dung cho moi bien the
        // (cung 1 san pham thi thanh phan nhu nhau, chi khac dung tich/mau).
        variant.setIngredients(blankToNull(form.getIngredients()));
        variant.setSuitableSkinType(form.getSuitableSkinTypes() == null || form.getSuitableSkinTypes().isEmpty()
                ? null : String.join(",", form.getSuitableSkinTypes()));
        variant.setIsActive(true);
    }

    /** SKU de trong thi tu tao; SKU da co thi kiem tra trung (tru bien the dang sua). */
    private String resolveVariantSku(String rawSku, ProductForm form, Integer excludeVariantId, int index) {
        String sku = rawSku == null ? "" : rawSku.trim().toUpperCase();
        if (sku.isBlank()) {
            String brand = form.getBrand() == null || form.getBrand().isBlank() ? "LUM" : form.getBrand();
            String prefix = FileStorageService.slug(brand).toUpperCase().replace("-", "");
            prefix = prefix.length() > 3 ? prefix.substring(0, 3) : prefix;
            String namePart = FileStorageService.slug(form.getName()).replace("-", "").toUpperCase();
            namePart = namePart.length() > 6 ? namePart.substring(0, 6) : namePart;
            sku = prefix + "-" + namePart + "-" + (System.currentTimeMillis() % 10000) + "-" + (index + 1);
        }
        Optional<ProductVariant> found = variants.findBySkuIgnoreCase(sku);
        boolean duplicate = found.map(v -> excludeVariantId == null || !v.getId().equals(excludeVariantId))
                .orElse(false);
        if (duplicate) {
            throw new IllegalArgumentException("Mã sản phẩm " + sku + " đã tồn tại");
        }
        return sku;
    }

    /** Luu lai toan bo thong so san pham: xoa cu, them moi theo thu tu nhap (dong trong thi bo qua). */
    private void saveAttributes(Product product, ProductForm form) {
        attributes.deleteByProductId(product.getId());
        if (form.getAttributes() == null) {
            return;
        }
        int order = 1;
        for (ProductForm.AttributeForm row : form.getAttributes()) {
            if (row == null || row.getName() == null || row.getName().isBlank()
                    || row.getValue() == null || row.getValue().isBlank()) {
                continue;
            }
            ProductAttribute attr = new ProductAttribute();
            attr.setProduct(product);
            attr.setName(row.getName().trim());
            attr.setValue(row.getValue().trim());
            attr.setDisplayOrder(order++);
            attributes.save(attr);
        }
    }

    /**
     * Luu media cho san pham MOI: anh chinh la image_1, cac anh phu tiep theo
     * la image_2, image_3..., video la video_1. Tra ve URL anh chinh (null neu
     * khong co file anh nao duoc tai len).
     */
    private String saveNewMedia(Product product, Category category,
                                MultipartFile image, MultipartFile[] images,
                                MultipartFile video, String videoUrl) {
        int imageIndex = 1;
        int order = 1;
        String mainImage = null;
        if (image != null && !image.isEmpty()) {
            mainImage = files.storeProductMedia(image, category, product.getId(), "images", imageIndex++);
            if (mainImage != null) {
                media.save(imageMedia(product, mainImage, order++));
            }
        }
        if (images != null) {
            for (MultipartFile file : images) {
                if (file == null || file.isEmpty()) {
                    continue;
                }
                String stored = files.storeProductMedia(file, category, product.getId(), "images", imageIndex++);
                if (stored != null) {
                    if (mainImage == null) {
                        mainImage = stored;
                    }
                    media.save(imageMedia(product, stored, order++));
                }
            }
        }
        saveVideoMedia(product, category, video, videoUrl, order);
        return mainImage;
    }

    /**
     * Them media cho san pham DA CO: anh moi noi tiep so thu tu hien co
     * (image_3, image_4...), video moi thay the video cu neu da co.
     * Tra ve URL anh chinh moi (null neu khong tai anh chinh).
     */
    private String appendMedia(Product product, Category category,
                               MultipartFile image, MultipartFile[] images,
                               MultipartFile video, String videoUrl) {
        List<ProductMedia> existing = media.findByProductIdOrderByDisplayOrderAsc(product.getId());
        long imageCount = existing.stream().filter(m -> "IMAGE".equals(m.getMediaType())).count();
        int imageIndex = (int) imageCount + 1;
        int order = existing.stream().mapToInt(ProductMedia::getDisplayOrder).max().orElse(0) + 1;
        String mainImage = null;
        if (image != null && !image.isEmpty()) {
            mainImage = files.storeProductMedia(image, category, product.getId(), "images", imageIndex++);
            if (mainImage != null) {
                media.save(imageMedia(product, mainImage, order++));
            }
        }
        if (images != null) {
            for (MultipartFile file : images) {
                if (file == null || file.isEmpty()) {
                    continue;
                }
                String stored = files.storeProductMedia(file, category, product.getId(), "images", imageIndex++);
                if (stored != null) {
                    if (mainImage == null) {
                        mainImage = stored;
                    }
                    media.save(imageMedia(product, stored, order++));
                }
            }
        }
        saveVideoMedia(product, category, video, videoUrl, order);
        return mainImage;
    }

    /** Luu video: uu tien file upload (video_1.mp4), neu khong co thi dung link videoUrl. */
    private void saveVideoMedia(Product product, Category category,
                                MultipartFile video, String videoUrl, int order) {
        if (video != null && !video.isEmpty()) {
            String stored = files.storeProductMedia(video, category, product.getId(), "videos", 1);
            if (stored == null) {
                return;
            }
            List<ProductMedia> existing = media.findByProductIdOrderByDisplayOrderAsc(product.getId());
            ProductMedia old = existing.stream()
                    .filter(m -> "VIDEO".equals(m.getMediaType())).findFirst().orElse(null);
            if (old != null) {
                old.setMediaUrl(stored);
                old.setThumbnailUrl(stored);
                media.save(old);
            } else {
                ProductMedia item = new ProductMedia();
                item.setProduct(product);
                item.setMediaUrl(stored);
                item.setMediaType("VIDEO");
                item.setDisplayOrder(order);
                media.save(item);
            }
            return;
        }
        if (videoUrl != null && !videoUrl.isBlank()) {
            List<ProductMedia> existing = media.findByProductIdOrderByDisplayOrderAsc(product.getId());
            boolean exists = existing.stream()
                    .anyMatch(m -> "VIDEO".equals(m.getMediaType()) && videoUrl.trim().equals(m.getMediaUrl()));
            if (!exists) {
                ProductMedia item = new ProductMedia();
                item.setProduct(product);
                item.setMediaUrl(videoUrl.trim());
                item.setMediaType("VIDEO");
                item.setDisplayOrder(order);
                media.save(item);
            }
        }
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
