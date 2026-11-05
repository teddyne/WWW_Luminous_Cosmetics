package com.luminous.service.admin;

import com.luminous.dto.VoucherForm;
import com.luminous.dto.VoucherRow;
import com.luminous.model.Category;
import com.luminous.model.enums.CategoryStatus;
import com.luminous.model.enums.DiscountType;
import com.luminous.model.Product;
import com.luminous.model.Voucher;
import com.luminous.model.enums.VoucherApplicableScope;
import com.luminous.model.VoucherCategory;
import com.luminous.model.VoucherProduct;
import com.luminous.model.enums.VoucherScope;
import com.luminous.model.VoucherType;
import com.luminous.repository.category.CategoryRepository;
import com.luminous.repository.product.ProductRepository;
import com.luminous.repository.voucher.VoucherCategoryRepository;
import com.luminous.repository.voucher.VoucherProductRepository;
import com.luminous.repository.voucher.VoucherRepository;
import com.luminous.repository.voucher.VoucherTypeRepository;
import com.luminous.service.FileStorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AdminVoucherService {

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final VoucherRepository vouchers;
    private final VoucherTypeRepository voucherTypes;
    private final VoucherCategoryRepository voucherCategories;
    private final VoucherProductRepository voucherProducts;
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final FileStorageService files;

    public AdminVoucherService(VoucherRepository vouchers,
                               VoucherTypeRepository voucherTypes,
                               VoucherCategoryRepository voucherCategories,
                               VoucherProductRepository voucherProducts,
                               CategoryRepository categories,
                               ProductRepository products,
                               FileStorageService files) {
        this.vouchers = vouchers;
        this.voucherTypes = voucherTypes;
        this.voucherCategories = voucherCategories;
        this.voucherProducts = voucherProducts;
        this.categories = categories;
        this.products = products;
        this.files = files;
    }

    public Page<VoucherRow> page(String q, Boolean active, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("endDate").descending());
        Page<Voucher> result = vouchers.findAll(spec(q, active), pageable);
        LocalDateTime now = LocalDateTime.now();
        return result.map(v -> toRow(v, now));
    }

    private Specification<Voucher> spec(String q, Boolean active) {
        return (root, query, cb) -> {
            List<Predicate> and = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                and.add(cb.or(
                        cb.like(cb.lower(root.get("voucherCode")), like),
                        cb.like(cb.lower(root.get("voucherName")), like)));
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
            Voucher voucher = vouchers.findById(id).orElse(null);
            if (voucher == null || Boolean.valueOf(active).equals(voucher.getIsActive())) {
                continue;
            }
            voucher.setIsActive(active);
            vouchers.save(voucher);
            changed++;
        }
        return changed;
    }

    public VoucherForm formFor(Integer id) {
        Voucher voucher = vouchers.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khuyến mãi"));
        VoucherForm form = new VoucherForm();
        form.setId(voucher.getId());
        form.setVoucherName(voucher.getVoucherName());
        form.setVoucherCode(voucher.getVoucherCode());
        form.setDescription(voucher.getDescription());
        form.setDiscountType(voucher.getDiscountType().name());
        form.setDiscountValue(voucher.getDiscountValue());
        form.setMaxDiscountAmount(voucher.getMaxDiscountAmount());
        form.setMinOrderValue(voucher.getMinOrderValue());
        form.setUsageLimit(voucher.getUsageLimit());
        form.setStartDate(voucher.getStartDate());
        form.setEndDate(voucher.getEndDate());
        form.setApplicableScope(voucher.getApplicableScope().name());
        form.setScopeType(voucher.getScopeType().name());
        form.setImgUrl(voucher.getImgUrl());
        form.setActive(voucher.getIsActive());
        form.setCategoryIds(voucherCategories.findByVoucherId(id).stream()
                .map(link -> link.getCategory().getId()).toList());
        form.setProductIds(voucherProducts.findByVoucherId(id).stream()
                .map(link -> link.getProduct().getId()).toList());
        return form;
    }

    public List<Category> categories() {
        return categories.findByStatusOrderByIdAsc(CategoryStatus.ACTIVE);
    }

    public List<Product> products() {
        return products.findByIsActiveTrueOrderByNameAsc();
    }

    @Transactional
    public Integer create(VoucherForm form, MultipartFile image) {
        validate(form, null);
        Voucher voucher = new Voucher();
        apply(voucher, form, image);
        voucher.setUsedCount(0);
        vouchers.save(voucher);
        saveLinks(voucher, form);
        return voucher.getId();
    }

    @Transactional
    public void update(Integer id, VoucherForm form, MultipartFile image) {
        Voucher voucher = vouchers.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khuyến mãi"));
        validate(form, id);
        apply(voucher, form, image);
        vouchers.save(voucher);
        voucherCategories.deleteByVoucherId(id);
        voucherProducts.deleteByVoucherId(id);
        saveLinks(voucher, form);
    }

    @Transactional
    public void toggleActive(Integer id) {
        Voucher voucher = vouchers.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khuyến mãi"));
        voucher.setIsActive(!Boolean.TRUE.equals(voucher.getIsActive()));
        vouchers.save(voucher);
    }

    @Transactional
    public void delete(Integer id) {
        Voucher voucher = vouchers.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khuyến mãi"));
        if (voucher.getUsedCount() != null && voucher.getUsedCount() > 0
                || vouchers.countOrderVoucherByVoucherId(id) > 0) {
            throw new IllegalArgumentException("Khuyến mãi đã được sử dụng nên chỉ có thể tạm dừng, không thể xóa");
        }
        voucherCategories.deleteByVoucherId(id);
        voucherProducts.deleteByVoucherId(id);
        vouchers.delete(voucher);
    }

    private void validate(VoucherForm form, Integer id) {
        if (form.getDiscountValue() == null || form.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Mức giảm phải lớn hơn 0");
        }
        if (form.getEndDate() != null && form.getStartDate() != null
                && !form.getEndDate().isAfter(form.getStartDate())) {
            throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu");
        }
        if (DiscountType.PERCENTAGE == DiscountType.from(form.getDiscountType())
                && form.getDiscountValue() != null
                && form.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Mức giảm theo phần trăm không được vượt quá 100%");
        }
        if (VoucherScope.CATEGORY == VoucherScope.from(form.getScopeType())
                && (form.getCategoryIds() == null || form.getCategoryIds().isEmpty())) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một loại sản phẩm áp dụng");
        }
        if (VoucherScope.PRODUCT == VoucherScope.from(form.getScopeType())
                && (form.getProductIds() == null || form.getProductIds().isEmpty())) {
            throw new IllegalArgumentException("Vui lòng chọn ít nhất một sản phẩm áp dụng");
        }
        String code = form.getVoucherCode() == null ? "" : form.getVoucherCode().trim().toUpperCase();
        if (!code.isBlank()) {
            boolean duplicate = vouchers.findByVoucherCodeIgnoreCase(code)
                    .map(other -> !other.getId().equals(id)).orElse(false);
            if (duplicate) {
                throw new IllegalArgumentException("Mã voucher đã tồn tại");
            }
        }
    }

    private void apply(Voucher voucher, VoucherForm form, MultipartFile image) {
        String code = form.getVoucherCode() == null ? "" : form.getVoucherCode().trim().toUpperCase();
        if (code.isBlank()) {
            code = generateCode();
        }
        DiscountType discountType = DiscountType.from(form.getDiscountType());
        if (discountType == null) {
            discountType = DiscountType.PERCENTAGE;
        }
        VoucherScope scope = VoucherScope.from(form.getScopeType());
        if (scope == null) {
            scope = VoucherScope.ALL;
        }
        VoucherApplicableScope applicable = VoucherApplicableScope.from(form.getApplicableScope());
        if (applicable == null) {
            applicable = VoucherApplicableScope.ALL;
        }
        String typeName = applicable == VoucherApplicableScope.SHIPPING
                ? "Miễn phí vận chuyển"
                : discountType == DiscountType.PERCENTAGE
                        ? "Giảm giá theo phần trăm" : "Giảm giá cố định";
        VoucherType type = voucherTypes.findByTypeName(typeName)
                .orElseThrow(() -> new IllegalArgumentException("Loại voucher chưa có trong dữ liệu mẫu"));
        voucher.setVoucherType(type);
        voucher.setVoucherCode(code);
        voucher.setVoucherName(form.getVoucherName().trim());
        voucher.setDescription(form.getDescription() == null || form.getDescription().isBlank()
                ? null : form.getDescription().trim());
        voucher.setDiscountType(discountType);
        voucher.setDiscountValue(form.getDiscountValue());
        voucher.setMaxDiscountAmount(form.getMaxDiscountAmount());
        if (discountType == DiscountType.FIXED_AMOUNT && form.getMaxDiscountAmount() == null) {
            voucher.setMaxDiscountAmount(form.getDiscountValue());
        }
        voucher.setMinOrderValue(form.getMinOrderValue() == null ? BigDecimal.ZERO : form.getMinOrderValue());
        voucher.setUsageLimit(form.getUsageLimit());
        voucher.setStartDate(form.getStartDate());
        voucher.setEndDate(form.getEndDate());
        voucher.setApplicableScope(applicable);
        voucher.setScopeType(scope);
        voucher.setIsActive(Boolean.TRUE.equals(form.getActive()));
        String storedImage = files.store(image, "vouchers", form.getVoucherName());
        if (storedImage != null) {
            voucher.setImgUrl(storedImage);
        } else if (form.getImgUrl() != null && !form.getImgUrl().isBlank()) {
            voucher.setImgUrl(form.getImgUrl().trim());
        }
    }

    private void saveLinks(Voucher voucher, VoucherForm form) {
        VoucherScope scope = VoucherScope.from(form.getScopeType());
        if (scope == null) {
            scope = VoucherScope.ALL;
        }
        if (scope == VoucherScope.CATEGORY && form.getCategoryIds() != null) {
            for (Integer categoryId : form.getCategoryIds()) {
                Category category = categories.findById(categoryId).orElse(null);
                if (category == null) {
                    continue;
                }
                VoucherCategory link = new VoucherCategory();
                link.setVoucher(voucher);
                link.setCategory(category);
                voucherCategories.save(link);
            }
        }
        if (scope == VoucherScope.PRODUCT && form.getProductIds() != null) {
            for (Integer productId : form.getProductIds()) {
                Product product = products.findById(productId).orElse(null);
                if (product == null) {
                    continue;
                }
                VoucherProduct link = new VoucherProduct();
                link.setVoucher(voucher);
                link.setProduct(product);
                voucherProducts.save(link);
            }
        }
    }

    private String generateCode() {
        String code;
        do {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                builder.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
            }
            code = builder.toString();
        } while (vouchers.existsByVoucherCodeIgnoreCase(code));
        return code;
    }

    private VoucherRow toRow(Voucher voucher, LocalDateTime now) {
        return new VoucherRow(
                voucher.getId(),
                voucher.getVoucherCode(),
                voucher.getVoucherName(),
                voucher.getDiscountType(),
                voucher.getDiscountValue(),
                voucher.getMaxDiscountAmount(),
                voucher.getMinOrderValue(),
                voucher.getUsageLimit() == null ? 0 : voucher.getUsageLimit(),
                voucher.getUsedCount() == null ? 0 : voucher.getUsedCount(),
                voucher.getStartDate(),
                voucher.getEndDate(),
                voucher.getScopeType(),
                Boolean.TRUE.equals(voucher.getIsActive()),
                voucher.displayStatus(now));
    }
}
