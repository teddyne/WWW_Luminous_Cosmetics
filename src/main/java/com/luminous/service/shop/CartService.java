package com.luminous.service.shop;

import com.luminous.dto.CartLine;
import com.luminous.dto.ProductCard;
import com.luminous.model.Cart;
import com.luminous.model.CartItem;
import com.luminous.model.ProductVariant;
import com.luminous.model.User;
import com.luminous.model.Voucher;
import com.luminous.model.enums.DiscountType;
import com.luminous.model.enums.VoucherApplicableScope;
import com.luminous.model.enums.VoucherScope;
import com.luminous.repository.cart.CartItemRepository;
import com.luminous.repository.cart.CartRepository;
import com.luminous.repository.product.ProductVariantRepository;
import com.luminous.repository.voucher.VoucherCategoryRepository;
import com.luminous.repository.voucher.VoucherProductRepository;
import com.luminous.repository.voucher.VoucherRepository;
import com.luminous.service.product.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Gio hang DB cho khach da dang nhap + ma giam gia ap dung o gio hang.
 * Ma da ap dung luu theo session (khoa SESSION_VOUCHER_CODE), chi ghi vao
 * OrderVoucher khi dat hang thanh cong (buoc checkout), khong tru usedCount o day.
 */
@Service
@Transactional
public class CartService {

    /** Ten khoa session luu ma giam gia dang ap dung cho gio hang. */
    public static final String SESSION_VOUCHER_CODE = "cartVoucherCode";

    /** Ten khoa session luu gio hang cua khach (variantId -> so luong). */
    public static final String SESSION_GUEST_CART = "guestCart";

    private final CartRepository carts;
    private final CartItemRepository cartItems;
    private final ProductVariantRepository variants;
    private final ProductService productService;
    private final VoucherRepository vouchers;
    private final VoucherCategoryRepository voucherCategories;
    private final VoucherProductRepository voucherProducts;

    public CartService(CartRepository carts,
                       CartItemRepository cartItems,
                       ProductVariantRepository variants,
                       ProductService productService,
                       VoucherRepository vouchers,
                       VoucherCategoryRepository voucherCategories,
                       VoucherProductRepository voucherProducts) {
        this.carts = carts;
        this.cartItems = cartItems;
        this.variants = variants;
        this.productService = productService;
        this.vouchers = vouchers;
        this.voucherCategories = voucherCategories;
        this.voucherProducts = voucherProducts;
    }

    /** Tong tien gio hang: tam tinh - giam gia, khong am. */
    public record Totals(BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
    }

    // ---------- Gio hang ----------

    public Cart getOrCreateCart(User user) {
        return carts.findByUserId(user.getId())
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUser(user);
                    return carts.save(cart);
                });
    }

    /**
     * Cac dong gio hang de hien. Bo qua dong co variant da ngung ban
     * (khong hien, khong tinh tien); dong het hang van hien de bao.
     */
    @Transactional(readOnly = true)
    public List<CartLine> lines(User user) {
        if (user == null) {
            return new ArrayList<>();
        }
        return carts.findByUserId(user.getId())
                .map(cart -> cartItems.findByCartIdOrderByIdAsc(cart.getId()).stream()
                        .map(item -> toLine(item))
                        .filter(line -> line != null)
                        .collect(Collectors.toList()))
                .orElseGet(ArrayList::new);
    }

    private CartLine toLine(CartItem item) {
        ProductVariant variant = item.getVariant();
        if (variant == null || !Boolean.TRUE.equals(variant.getIsActive())) {
            return null;
        }
        return toLine(variant, item.getQuantity());
    }

    private CartLine toLine(ProductVariant variant, int quantity) {
        ProductCard card = productService.cardForVariant(variant);
        BigDecimal unit = card.displayPrice() != null ? card.displayPrice() : BigDecimal.ZERO;
        BigDecimal lineTotal = unit.multiply(BigDecimal.valueOf(quantity));
        return new CartLine(card, quantity, lineTotal);
    }

    /** Them variant vao gio; trung variant thi cong don so luong (toi da = ton kho). */
    public boolean addItem(User user, Integer variantId, int quantity) {
        if (user == null) {
            return false;
        }
        Optional<ProductVariant> found = variants.findById(variantId);
        if (found.isEmpty()) {
            return false;
        }
        ProductVariant variant = found.get();
        if (!Boolean.TRUE.equals(variant.getIsActive())
                || variant.getStockQuantity() == null || variant.getStockQuantity() <= 0) {
            return false;
        }
        Cart cart = getOrCreateCart(user);
        int wanted = Math.max(1, quantity);
        int allowed = Math.min(wanted, variant.getStockQuantity());
        cartItems.findByCartIdAndVariantId(cart.getId(), variantId)
                .ifPresentOrElse(
                        item -> {
                            int next = Math.min(item.getQuantity() + allowed, variant.getStockQuantity());
                            item.setQuantity(next);
                            cartItems.save(item);
                        },
                        () -> {
                            CartItem item = new CartItem();
                            item.setCart(cart);
                            item.setVariant(variant);
                            item.setQuantity(allowed);
                            cartItems.save(item);
                        });
        return true;
    }

    /** Doi so luong 1 dong (kep trong [1, ton kho]); het hang thi xoa dong do. */
    public boolean updateQuantity(User user, Integer variantId, int quantity) {
        if (user == null) {
            return false;
        }
        Cart cart = carts.findByUserId(user.getId()).orElse(null);
        if (cart == null) {
            return false;
        }
        Optional<CartItem> found = cartItems.findByCartIdAndVariantId(cart.getId(), variantId);
        if (found.isEmpty()) {
            return false;
        }
        CartItem item = found.get();
        ProductVariant variant = item.getVariant();
        if (variant == null || variant.getStockQuantity() == null || variant.getStockQuantity() <= 0) {
            cartItems.delete(item);
            return true;
        }
        int next = Math.min(Math.max(1, quantity), variant.getStockQuantity());
        item.setQuantity(next);
        cartItems.save(item);
        return true;
    }

    public void removeItem(User user, Integer variantId) {
        if (user == null) {
            return;
        }
        carts.findByUserId(user.getId())
                .ifPresent(cart -> cartItems.deleteByCartIdAndVariantId(cart.getId(), variantId));
    }

    public void removeItems(User user, List<Integer> variantIds) {
        if (user == null || variantIds == null || variantIds.isEmpty()) {
            return;
        }
        carts.findByUserId(user.getId()).ifPresent(cart -> variantIds.forEach(
                variantId -> cartItems.deleteByCartIdAndVariantId(cart.getId(), variantId)));
    }

    /** Tong so luong san pham trong gio (de hien huy hieu tren icon gio hang). */
    @Transactional(readOnly = true)
    public int countItems(User user) {
        if (user == null) {
            return 0;
        }
        return carts.findByUserId(user.getId())
                .map(cart -> cartItems.findByCartIdOrderByIdAsc(cart.getId()).stream()
                        .mapToInt(item -> item.getQuantity() == null ? 0 : item.getQuantity())
                        .sum())
                .orElse(0);
    }

    // ---------- Gio hang cua khach (luu trong session) ----------

    @SuppressWarnings("unchecked")
    private java.util.LinkedHashMap<Integer, Integer> guestCartMap(jakarta.servlet.http.HttpSession session) {
        Object value = session.getAttribute(SESSION_GUEST_CART);
        if (value instanceof java.util.LinkedHashMap<?, ?> map) {
            return (java.util.LinkedHashMap<Integer, Integer>) map;
        }
        java.util.LinkedHashMap<Integer, Integer> cart = new java.util.LinkedHashMap<>();
        session.setAttribute(SESSION_GUEST_CART, cart);
        return cart;
    }

    /** Cac dong gio hang cua khach, giai tu session (bo variant da ngung ban). */
    @Transactional(readOnly = true)
    public List<CartLine> guestLines(jakarta.servlet.http.HttpSession session) {
        List<CartLine> lines = new ArrayList<>();
        for (java.util.Map.Entry<Integer, Integer> entry : guestCartMap(session).entrySet()) {
            Optional<ProductVariant> found = variants.findById(entry.getKey());
            if (found.isEmpty()) {
                continue;
            }
            ProductVariant variant = found.get();
            if (!Boolean.TRUE.equals(variant.getIsActive())) {
                continue;
            }
            lines.add(toLine(variant, entry.getValue()));
        }
        return lines;
    }

    /** Them variant vao gio khach; trung variant thi cong don (toi da = ton kho). */
    public boolean addGuestItem(jakarta.servlet.http.HttpSession session, Integer variantId, int quantity) {
        Optional<ProductVariant> found = variants.findById(variantId);
        if (found.isEmpty()) {
            return false;
        }
        ProductVariant variant = found.get();
        if (!Boolean.TRUE.equals(variant.getIsActive())
                || variant.getStockQuantity() == null || variant.getStockQuantity() <= 0) {
            return false;
        }
        java.util.LinkedHashMap<Integer, Integer> map = guestCartMap(session);
        int allowed = Math.min(Math.max(1, quantity), variant.getStockQuantity());
        map.merge(variantId, allowed,
                (oldQty, add) -> Math.min(oldQty + add, variant.getStockQuantity()));
        return true;
    }

    /** Doi so luong 1 dong cua khach (kep trong [1, ton kho]); het hang thi xoa dong do. */
    public void updateGuestQuantity(jakarta.servlet.http.HttpSession session, Integer variantId, int quantity) {
        java.util.LinkedHashMap<Integer, Integer> map = guestCartMap(session);
        if (!map.containsKey(variantId)) {
            return;
        }
        Optional<ProductVariant> found = variants.findById(variantId);
        if (found.isEmpty() || found.get().getStockQuantity() == null
                || found.get().getStockQuantity() <= 0) {
            map.remove(variantId);
            return;
        }
        map.put(variantId, Math.min(Math.max(1, quantity), found.get().getStockQuantity()));
    }

    public void removeGuestItem(jakarta.servlet.http.HttpSession session, Integer variantId) {
        guestCartMap(session).remove(variantId);
    }

    public void removeGuestItems(jakarta.servlet.http.HttpSession session, List<Integer> variantIds) {
        if (variantIds == null || variantIds.isEmpty()) {
            return;
        }
        java.util.LinkedHashMap<Integer, Integer> map = guestCartMap(session);
        variantIds.forEach(map::remove);
    }

    /** Tong so luong san pham trong gio khach (de hien huy hieu tren icon gio hang). */
    public int countGuestItems(jakarta.servlet.http.HttpSession session) {
        if (session == null) {
            return 0;
        }
        Object value = session.getAttribute(SESSION_GUEST_CART);
        if (!(value instanceof java.util.LinkedHashMap<?, ?> map)) {
            return 0;
        }
        return map.values().stream()
                .filter(Integer.class::isInstance)
                .mapToInt(v -> (Integer) v)
                .sum();
    }

    /** Khach dang nhap: chuyen gio tam trong session vao gio DB roi xoa session. */
    public void mergeGuestCart(User user, jakarta.servlet.http.HttpSession session) {
        if (user == null || session == null) {
            return;
        }
        Object value = session.getAttribute(SESSION_GUEST_CART);
        if (!(value instanceof java.util.LinkedHashMap<?, ?> map) || map.isEmpty()) {
            return;
        }
        map.forEach((variantId, quantity) -> {
            if (variantId instanceof Integer id && quantity instanceof Integer qty) {
                addItem(user, id, qty);
            }
        });
        session.removeAttribute(SESSION_GUEST_CART);
    }


    // ---------- Tong tien ----------

    /** Tam tinh = tong thanh tien cac dong con hang (dong het hang khong tinh). */
    public BigDecimal subtotal(List<CartLine> lines) {
        return lines.stream()
                .filter(CartLine::available)
                .map(CartLine::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public Totals totals(List<CartLine> lines, Voucher applied) {
        BigDecimal sub = subtotal(lines);
        BigDecimal discount = applied == null ? BigDecimal.ZERO : discountFor(applied, lines);
        if (discount.compareTo(sub) > 0) {
            discount = sub;
        }
        return new Totals(sub, discount, sub.subtract(discount));
    }

    // ---------- Ma giam gia ----------

    @Transactional(readOnly = true)
    public Optional<Voucher> findVoucherByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return vouchers.findByVoucherCodeIgnoreCase(code.trim());
    }

    /** Cac id danh muc ma voucher gioi han (scopeType = CATEGORY). */
    @Transactional(readOnly = true)
    public Set<Integer> scopeCategoryIds(Voucher voucher) {
        return voucherCategories.findByVoucherId(voucher.getId()).stream()
                .map(link -> link.getCategory().getId())
                .collect(Collectors.toSet());
    }

    /** Cac id san pham ma voucher gioi han (scopeType = PRODUCT). */
    @Transactional(readOnly = true)
    public Set<Integer> scopeProductIds(Voucher voucher) {
        return voucherProducts.findByVoucherId(voucher.getId()).stream()
                .map(link -> link.getProduct().getId())
                .collect(Collectors.toSet());
    }

    /**
     * Phan tam tinh ma voucher duoc phep giam: ALL = toan bo gio;
     * CATEGORY = chi dong thuoc danh muc duoc lien ket;
     * PRODUCT = chi dong la san pham duoc lien ket.
     */
    public BigDecimal eligibleBase(Voucher voucher, List<CartLine> lines) {
        if (voucher.getScopeType() == VoucherScope.CATEGORY) {
            Set<Integer> allowed = scopeCategoryIds(voucher);
            return lines.stream()
                    .filter(CartLine::available)
                    .filter(line -> line.card().getProduct().getCategory() != null
                            && allowed.contains(line.card().getProduct().getCategory().getId()))
                    .map(CartLine::lineTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        if (voucher.getScopeType() == VoucherScope.PRODUCT) {
            Set<Integer> allowed = scopeProductIds(voucher);
            return lines.stream()
                    .filter(CartLine::available)
                    .filter(line -> allowed.contains(line.card().getProduct().getId()))
                    .map(CartLine::lineTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return subtotal(lines);
    }

    /**
     * Kiem tra ma giam gia voi gio hang hien tai.
     * Tra ve null = hop le; khac null = ly do khong dung duoc (hien cho nguoi dung).
     */
    public String validateVoucher(Voucher voucher, List<CartLine> lines, LocalDateTime now) {
        if (voucher == null) {
            return "Mã giảm giá không tồn tại.";
        }
        if (!Boolean.TRUE.equals(voucher.getIsActive())) {
            return "Mã giảm giá đã bị vô hiệu hoá.";
        }
        if (now.isBefore(voucher.getStartDate())) {
            return "Mã giảm giá chưa tới thời gian áp dụng.";
        }
        if (now.isAfter(voucher.getEndDate())) {
            return "Mã giảm giá đã hết hạn.";
        }
        if (voucher.getUsedCount() != null && voucher.getUsageLimit() != null
                && voucher.getUsedCount() >= voucher.getUsageLimit()) {
            return "Mã giảm giá đã hết lượt sử dụng.";
        }
        if (voucher.getApplicableScope() == VoucherApplicableScope.SHIPPING) {
            return "Mã này chỉ giảm phí vận chuyển, sẽ áp dụng ở bước thanh toán.";
        }
        BigDecimal base = eligibleBase(voucher, lines);
        if (base.compareTo(BigDecimal.ZERO) <= 0) {
            return "Mã giảm giá không áp dụng cho sản phẩm trong giỏ hàng.";
        }
        if (voucher.getMinOrderValue() != null && base.compareTo(voucher.getMinOrderValue()) < 0) {
            return "Đơn hàng chưa đủ " + CartLine.formatVnd(voucher.getMinOrderValue())
                    + " để dùng mã này.";
        }
        return null;
    }

    /** So tien duoc giam (da kep theo maxDiscountAmount va khong vuot phan duoc giam). */
    public BigDecimal discountFor(Voucher voucher, List<CartLine> lines) {
        BigDecimal base = eligibleBase(voucher, lines);
        if (base.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal discount;
        if (voucher.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = base.multiply(voucher.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            discount = voucher.getDiscountValue();
        }
        if (voucher.getMaxDiscountAmount() != null && discount.compareTo(voucher.getMaxDiscountAmount()) > 0) {
            discount = voucher.getMaxDiscountAmount();
        }
        if (discount.compareTo(base) > 0) {
            discount = base;
        }
        return discount;
    }
}
