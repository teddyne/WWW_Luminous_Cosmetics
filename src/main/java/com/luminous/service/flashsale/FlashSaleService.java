package com.luminous.service.flashsale;

import com.luminous.dto.FlashSaleForm;
import com.luminous.dto.ProductCard;
import com.luminous.model.FlashSale;
import com.luminous.model.FlashSaleItem;
import com.luminous.model.Product;
import com.luminous.model.ProductMedia;
import com.luminous.model.ProductVariant;
import com.luminous.model.enums.FlashSaleStatus;
import com.luminous.model.enums.OrderStatus;
import com.luminous.repository.flashsale.FlashSaleItemRepository;
import com.luminous.repository.flashsale.FlashSaleRepository;
import com.luminous.repository.order.OrderDetailRepository;
import com.luminous.repository.product.ProductRepository;
import com.luminous.repository.product.ProductVariantRepository;
import com.luminous.service.FileStorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class FlashSaleService {

    private static final long PRICE_CACHE_TTL_MS = 30_000;
    private static final List<OrderStatus> EXCLUDED_ORDER_STATUS =
            List.of(OrderStatus.CANCELLED, OrderStatus.RETURNED);

    private final FlashSaleRepository sales;
    private final FlashSaleItemRepository items;
    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final OrderDetailRepository orderDetails;
    private final FileStorageService files;

    private volatile Map<Integer, FlashPrice> priceCache = Map.of();
    private volatile long priceCacheAt = 0;

    public FlashSaleService(FlashSaleRepository sales, FlashSaleItemRepository items,
                            ProductRepository products, ProductVariantRepository variants,
                            OrderDetailRepository orderDetails, FileStorageService files) {
        this.sales = sales;
        this.items = items;
        this.products = products;
        this.variants = variants;
        this.orderDetails = orderDetails;
        this.files = files;
    }

    public record FlashPrice(BigDecimal price, int discountPercent, Integer promoQuantity,
                             long soldQuantity, Integer maxPerCustomer,
                             Integer saleId, String saleName) {
    }

    public Optional<FlashSale> activeSale() {
        LocalDateTime now = LocalDateTime.now();
        return sales.findLive(FlashSaleStatus.PUBLISHED, now).stream().findFirst();
    }

    public Optional<FlashSale> upcomingSale() {
        LocalDateTime now = LocalDateTime.now();
        return sales.findUpcoming(FlashSaleStatus.PUBLISHED, now).stream().findFirst();
    }

    public Optional<FlashSale> showcaseSale() {
        Optional<FlashSale> active = activeSale();
        return active.isPresent() ? active : upcomingSale();
    }

    public Optional<FlashPrice> flashPrice(Integer variantId) {
        if (variantId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(priceMap().get(variantId));
    }

    public BigDecimal flashPriceForItem(FlashSaleItem item) {
        ProductVariant variant = item.getVariant();
        if (variant == null || variant.getPrice() == null) {
            return null;
        }
        BigDecimal flash = variant.getPrice()
                .multiply(BigDecimal.valueOf(100 - item.getDiscountPercent()))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        BigDecimal display = variant.displayPrice();
        return display != null && display.compareTo(flash) < 0 ? display : flash;
    }

    public Page<FlashSale> search(String q, String display, int page, int size) {
        int safeSize = (size == 10 || size == 20 || size == 100) ? size : 10;
        return sales.search(q == null ? "" : q.trim(), display == null ? "" : display,
                LocalDateTime.now(), PageRequest.of(Math.max(0, page), safeSize));
    }

    public FlashSale getOrThrow(Integer id) {
        return sales.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chương trình Flash Sale"));
    }

    public List<FlashSaleItem> itemsOf(Integer saleId) {
        return items.findByFlashSaleIdOrderByIdAsc(saleId);
    }

    public long soldCount(FlashSale sale, FlashSaleItem item) {
        return orderDetails.soldInPeriod(item.getVariant().getId(),
                sale.getStartAt(), sale.getEndAt(), EXCLUDED_ORDER_STATUS);
    }

    public long boughtByUser(Integer userId, Integer variantId, FlashSale sale) {
        return orderDetails.boughtByUserInPeriod(variantId, userId,
                sale.getStartAt(), sale.getEndAt(), EXCLUDED_ORDER_STATUS);
    }

    public List<FlashSaleForm.ProductOption> productOptions() {
        List<Product> active = products.findByIsActiveTrueOrderByNameAsc();
        List<Integer> ids = active.stream().map(Product::getId).toList();
        Map<Integer, List<ProductVariant>> byProduct = new HashMap<>();
        if (!ids.isEmpty()) {
            for (ProductVariant v : variants.findByProductIdIn(ids)) {
                if (Boolean.TRUE.equals(v.getIsActive())
                        && v.getStockQuantity() != null && v.getStockQuantity() > 0) {
                    byProduct.computeIfAbsent(v.getProduct().getId(), k -> new ArrayList<>()).add(v);
                }
            }
        }
        List<FlashSaleForm.ProductOption> result = new ArrayList<>();
        for (Product p : active) {
            List<ProductVariant> vs = byProduct.getOrDefault(p.getId(), List.of());
            if (!vs.isEmpty()) {
                vs.sort(Comparator.comparing(ProductVariant::getPrice,
                        Comparator.nullsLast(Comparator.naturalOrder())));
                result.add(new FlashSaleForm.ProductOption(p, vs));
            }
        }
        return result;
    }

    public List<String> brands() {
        return productOptions().stream()
                .map(option -> option.product().getBrand())
                .filter(brand -> brand != null && !brand.isBlank())
                .distinct()
                .sorted(String::compareToIgnoreCase)
                .toList();
    }

    public List<FlashSaleForm.VariantOption> flatVariants() {
        List<FlashSaleForm.VariantOption> result = new ArrayList<>();
        for (FlashSaleForm.ProductOption option : productOptions()) {
            for (ProductVariant v : option.variants()) {
                result.add(new FlashSaleForm.VariantOption(v.getId(), option.product().getId(),
                        option.product().getName(), option.product().getBrand(),
                        v.getSize(), v.getPrice(), v.getStockQuantity(),
                        variantImageUrl(v, option.product())));
            }
        }
        return result;
    }

    private String variantImageUrl(ProductVariant variant, Product product) {
        if (variant.getImgUrl() != null && !variant.getImgUrl().isBlank()) {
            return variant.getImgUrl();
        }
        if (product.getMedia() != null) {
            for (ProductMedia media : product.getMedia()) {
                if ("IMAGE".equals(media.getMediaType())
                        && media.getMediaUrl() != null && !media.getMediaUrl().isBlank()) {
                    return media.getMediaUrl();
                }
            }
        }
        return "/images/placeholder.svg";
    }

    public FlashSaleForm formFor(Integer id) {
        FlashSale sale = getOrThrow(id);
        FlashSaleForm form = new FlashSaleForm();
        form.setId(sale.getId());
        form.setVersion(sale.getVersion());
        form.setName(sale.getName());
        form.setDescription(sale.getDescription());
        form.setStartAt(sale.getStartAt());
        form.setEndAt(sale.getEndAt());
        form.setStatus(sale.getStatus().name());
        form.setBannerUrl(sale.getBannerUrl());
        Set<Integer> chosen = new HashSet<>();
        Map<Integer, FlashSaleItem> byVariant = new HashMap<>();
        for (FlashSaleItem item : itemsOf(id)) {
            chosen.add(item.getVariant().getId());
            byVariant.put(item.getVariant().getId(), item);
        }
        List<FlashSaleForm.ItemForm> rows = new ArrayList<>();
        for (FlashSaleForm.ProductOption option : productOptions()) {
            for (ProductVariant v : option.variants()) {
                FlashSaleForm.ItemForm row = new FlashSaleForm.ItemForm();
                row.setVariantId(v.getId());
                FlashSaleItem saved = byVariant.get(v.getId());
                if (saved != null) {
                    row.setSelected(true);
                    row.setDiscountPercent(saved.getDiscountPercent());
                    row.setPromoQuantity(saved.getPromoQuantity());
                    row.setMaxPerCustomer(saved.getMaxPerCustomer());
                }
                rows.add(row);
            }
        }
        form.setItems(rows);
        return form;
    }

    public FlashSaleForm blankForm() {
        FlashSaleForm form = new FlashSaleForm();
        form.setStartAt(LocalDateTime.now().withSecond(0).withNano(0));
        form.setEndAt(LocalDateTime.now().plusDays(7).withSecond(0).withNano(0));
        List<FlashSaleForm.ItemForm> rows = new ArrayList<>();
        for (FlashSaleForm.ProductOption option : productOptions()) {
            for (ProductVariant v : option.variants()) {
                FlashSaleForm.ItemForm row = new FlashSaleForm.ItemForm();
                row.setVariantId(v.getId());
                rows.add(row);
            }
        }
        form.setItems(rows);
        return form;
    }

    @Transactional
    public FlashSale create(FlashSaleForm form, MultipartFile banner) {
        validate(form);
        FlashSale sale = new FlashSale();
        applyForm(sale, form);
        sale.setCreatedAt(LocalDateTime.now());
        sale.setUpdatedAt(LocalDateTime.now());
        if (banner != null && !banner.isEmpty()) {
            sale.setBannerUrl(files.storePromoImage(banner, form.getName()));
        }
        FlashSale saved = sales.save(sale);
        saveItems(saved, form.selectedItems());
        invalidatePriceCache();
        return saved;
    }

    @Transactional
    public FlashSale update(Integer id, FlashSaleForm form, MultipartFile banner) {
        validate(form);
        FlashSale sale = getOrThrow(id);
        if (form.getVersion() != null) {
            sale.setVersion(form.getVersion());
        }
        String oldBanner = sale.getBannerUrl();
        applyForm(sale, form);
        sale.setUpdatedAt(LocalDateTime.now());
        if (banner != null && !banner.isEmpty()) {
            sale.setBannerUrl(files.storePromoImage(banner, form.getName()));
            deleteBannerFile(oldBanner);
        }
        FlashSale saved = sales.save(sale);
        items.deleteByFlashSaleId(id);
        // deleteBy... cua Spring Data chi goi em.remove() (xoa that luc flush),
        // ma Hibernate flush INSERT truoc DELETE nen phai day DELETE xuong DB
        // ngay tai day, khong thi chen muc moi se dung khoa uq_flash_item.
        items.flush();
        saveItems(saved, form.selectedItems());
        invalidatePriceCache();
        return saved;
    }

    @Transactional
    public void setStatus(Integer id, FlashSaleStatus status) {
        FlashSale sale = getOrThrow(id);
        sale.setStatus(status);
        sale.setUpdatedAt(LocalDateTime.now());
        sales.save(sale);
        invalidatePriceCache();
    }

    @Transactional
    public void delete(Integer id) {
        FlashSale sale = getOrThrow(id);
        for (FlashSaleItem item : itemsOf(id)) {
            if (soldCount(sale, item) > 0) {
                throw new IllegalArgumentException("Không thể xóa chương trình đã phát sinh đơn hàng"
                        + " (sản phẩm " + item.getVariant().getProduct().getName() + " đã bán trong chương trình).");
            }
        }
        String banner = sale.getBannerUrl();
        items.deleteByFlashSaleId(id);
        sales.delete(sale);
        deleteBannerFile(banner);
        invalidatePriceCache();
    }

    public void invalidatePriceCache() {
        priceCacheAt = 0;
    }

    private Map<Integer, FlashPrice> priceMap() {
        long nowMs = System.currentTimeMillis();
        if (nowMs - priceCacheAt < PRICE_CACHE_TTL_MS) {
            return priceCache;
        }
        synchronized (this) {
            if (System.currentTimeMillis() - priceCacheAt < PRICE_CACHE_TTL_MS) {
                return priceCache;
            }
            priceCache = buildPriceMap(LocalDateTime.now());
            priceCacheAt = System.currentTimeMillis();
            return priceCache;
        }
    }

    private Map<Integer, FlashPrice> buildPriceMap(LocalDateTime now) {
        Map<Integer, FlashPrice> result = new HashMap<>();
        for (FlashSaleItem item : items.findLiveItems(now)) {
            FlashSale sale = item.getFlashSale();
            ProductVariant variant = item.getVariant();
            if (variant.getPrice() == null) {
                continue;
            }
            long sold = orderDetails.soldInPeriod(variant.getId(),
                    sale.getStartAt(), sale.getEndAt(), EXCLUDED_ORDER_STATUS);
            if (sold >= item.getPromoQuantity()) {
                continue;
            }
            BigDecimal flash = variant.getPrice()
                    .multiply(BigDecimal.valueOf(100 - item.getDiscountPercent()))
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            BigDecimal display = variant.displayPrice();
            BigDecimal effective = display != null && display.compareTo(flash) < 0 ? display : flash;
            int percent = ProductCard.discountPercentOf(variant.getPrice(), effective);
            FlashPrice candidate = new FlashPrice(effective, percent, item.getPromoQuantity(),
                    sold, item.getMaxPerCustomer(), sale.getId(), sale.getName());
            FlashPrice current = result.get(variant.getId());
            if (current == null || effective.compareTo(current.price()) < 0) {
                result.put(variant.getId(), candidate);
            }
        }
        return result;
    }

    private void applyForm(FlashSale sale, FlashSaleForm form) {
        sale.setName(form.getName().trim());
        sale.setDescription(form.getDescription() == null || form.getDescription().isBlank()
                ? null : form.getDescription().trim());
        sale.setStartAt(form.getStartAt());
        sale.setEndAt(form.getEndAt());
        sale.setStatus(FlashSaleStatus.valueOf(form.getStatus()));
        if (form.getBannerUrl() != null && !form.getBannerUrl().isBlank()) {
            sale.setBannerUrl(form.getBannerUrl().trim());
        }
    }

    private void saveItems(FlashSale sale, List<FlashSaleForm.ItemForm> selected) {
        for (FlashSaleForm.ItemForm row : selected) {
            ProductVariant variant = variants.findById(row.getVariantId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Không tìm thấy biến thể sản phẩm #" + row.getVariantId()));
            if (!Boolean.TRUE.equals(variant.getIsActive())) {
                throw new IllegalArgumentException(
                        "Biến thể " + variant.getProduct().getName() + " đã ngừng bán.");
            }
            FlashSaleItem item = new FlashSaleItem();
            item.setFlashSale(sale);
            item.setVariant(variant);
            item.setDiscountPercent(row.getDiscountPercent());
            item.setPromoQuantity(row.getPromoQuantity());
            item.setMaxPerCustomer(row.getMaxPerCustomer());
            items.save(item);
        }
    }

    private void validate(FlashSaleForm form) {
        if (form.getName() == null || form.getName().isBlank()) {
            throw new IllegalArgumentException("Tên chương trình không được để trống");
        }
        if (form.getStartAt() == null || form.getEndAt() == null) {
            throw new IllegalArgumentException("Vui lòng chọn thời gian bắt đầu và kết thúc");
        }
        if (!form.getEndAt().isAfter(form.getStartAt())) {
            throw new IllegalArgumentException("Thời gian kết thúc phải sau thời gian bắt đầu");
        }
        try {
            FlashSaleStatus.valueOf(form.getStatus());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ");
        }
        List<FlashSaleForm.ItemForm> selected = form.selectedItems();
        if (selected.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất 1 sản phẩm cho chương trình");
        }
        Set<Integer> seen = new HashSet<>();
        for (FlashSaleForm.ItemForm row : selected) {
            if (row.getVariantId() == null || !seen.add(row.getVariantId())) {
                throw new IllegalArgumentException("Danh sách sản phẩm có mục trùng lặp");
            }
            if (row.getDiscountPercent() == null
                    || row.getDiscountPercent() < 1 || row.getDiscountPercent() > 99) {
                throw new IllegalArgumentException("Phần trăm giảm phải từ 1% đến 99%");
            }
            if (row.getPromoQuantity() == null || row.getPromoQuantity() < 1) {
                throw new IllegalArgumentException("Số lượng khuyến mãi phải từ 1 trở lên");
            }
            if (row.getMaxPerCustomer() != null && row.getMaxPerCustomer() < 1) {
                throw new IllegalArgumentException("Giới hạn mua phải từ 1 trở lên");
            }
        }
    }

    private void deleteBannerFile(String bannerUrl) {
        if (bannerUrl == null || !bannerUrl.startsWith("/images/promo/flash-sale-")) {
            return;
        }
        try {
            Path file = Paths.get("src/main/resources/static", bannerUrl);
            Files.deleteIfExists(file);
        } catch (Exception ignored) {
            // Xoa file banner that bai khong duoc chan nghiep vu chinh.
        }
    }
}
