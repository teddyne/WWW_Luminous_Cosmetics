package com.luminous.service.shop;

import com.luminous.dto.CartLine;
import com.luminous.model.Address;
import com.luminous.model.Cart;
import com.luminous.model.OrderDetail;
import com.luminous.model.OrderVoucher;
import com.luminous.model.Orders;
import com.luminous.model.Payment;
import com.luminous.model.PointTransaction;
import com.luminous.model.ProductVariant;
import com.luminous.model.User;
import com.luminous.model.Voucher;
import com.luminous.model.enums.OrderStatus;
import com.luminous.model.enums.PaymentMethod;
import com.luminous.model.enums.PaymentStatus;
import com.luminous.repository.UserRepository;
import com.luminous.repository.address.AddressRepository;
import com.luminous.repository.cart.CartItemRepository;
import com.luminous.repository.cart.CartRepository;
import com.luminous.repository.order.OrderDetailRepository;
import com.luminous.repository.order.OrderRepository;
import com.luminous.repository.order.OrderVoucherRepository;
import com.luminous.repository.order.PaymentRepository;
import com.luminous.repository.order.PointTransactionRepository;
import com.luminous.repository.product.ProductVariantRepository;
import com.luminous.repository.voucher.VoucherRepository;
import com.luminous.service.notification.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Dat hang: tao don + chi tiet + thanh toan + voucher/diem trong 1 giao dich.
 * Phi ship co dinh 30.000đ; 1 diem = 1đ; gio giao ghi vao dau Orders.note
 * (bang Orders khong co cot gio giao rieng). Loi nghiep vu nem
 * IllegalArgumentException voi thong bao tieng Viet de controller hien lai.
 */
@Service
@Transactional
public class CheckoutService {

    /** Phi van chuyen co dinh cho moi don (chua co bang phi theo khu vuc). */
    public static final BigDecimal SHIPPING_FEE = new BigDecimal("30000");

    private final CartService cartService;
    private final CartRepository carts;
    private final CartItemRepository cartItems;
    private final ProductVariantRepository variants;
    private final AddressRepository addresses;
    private final OrderRepository orders;
    private final OrderDetailRepository orderDetails;
    private final PaymentRepository payments;
    private final OrderVoucherRepository orderVouchers;
    private final PointTransactionRepository pointTransactions;
    private final VoucherRepository vouchers;
    private final UserRepository users;
    private final NotificationService notifications;

    public CheckoutService(CartService cartService,
                           CartRepository carts,
                           CartItemRepository cartItems,
                           ProductVariantRepository variants,
                           AddressRepository addresses,
                           OrderRepository orders,
                           OrderDetailRepository orderDetails,
                           PaymentRepository payments,
                           OrderVoucherRepository orderVouchers,
                           PointTransactionRepository pointTransactions,
                           VoucherRepository vouchers,
                           UserRepository users,
                           NotificationService notifications) {
        this.cartService = cartService;
        this.carts = carts;
        this.cartItems = cartItems;
        this.variants = variants;
        this.addresses = addresses;
        this.orders = orders;
        this.orderDetails = orderDetails;
        this.payments = payments;
        this.orderVouchers = orderVouchers;
        this.pointTransactions = pointTransactions;
        this.vouchers = vouchers;
        this.users = users;
        this.notifications = notifications;
    }

    /** Ket qua dat hang: don vua tao + cac dong da dat + tong tien phai tra. */
    public record PlacedOrder(Orders order, List<CartLine> lines, BigDecimal total) {
    }

    /** Cac dong duoc chon de dat hang (chi dong con hang). */
    @Transactional(readOnly = true)
    public List<CartLine> selectedLines(User user, List<Integer> selectedVariantIds) {
        if (user == null || selectedVariantIds == null || selectedVariantIds.isEmpty()) {
            return List.of();
        }
        return cartService.lines(user).stream()
                .filter(l -> l.available()
                        && selectedVariantIds.contains(l.card().getVariant().getId()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<Orders> findByCode(String orderCode) {
        return orders.findByOrderCode(orderCode);
    }

    @Transactional(readOnly = true)
    public List<OrderDetail> detailsOf(Integer orderId) {
        return orderDetails.findByOrderIdOrderByIdAsc(orderId);
    }

    @Transactional(readOnly = true)
    public Optional<Payment> paymentOf(Integer orderId) {
        return payments.findTopByOrderIdOrderByIdDesc(orderId);
    }

    /**
     * Dat hang cho cac dong duoc chon. Tao don o trang thai PENDING + ban ghi
     * thanh toan PENDING; tru kho, tru diem, ghi voucher, xoa khoi gio.
     */
    public PlacedOrder placeOrder(User user, List<Integer> selectedVariantIds,
                                  Integer addressId, String deliverySlot, String note,
                                  int usePoints, PaymentMethod method, String voucherCode) {
        if (user == null) {
            throw new IllegalArgumentException("Bạn cần đăng nhập để đặt hàng.");
        }
        if (selectedVariantIds == null || selectedVariantIds.isEmpty()) {
            throw new IllegalArgumentException("Bạn chưa chọn sản phẩm nào để đặt hàng.");
        }
        if (method == null) {
            throw new IllegalArgumentException("Bạn chưa chọn phương thức thanh toán.");
        }

        // Dia chi phai la cua chinh khach nay.
        Address address = addresses.findByIdAndUserId(addressId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Địa chỉ giao hàng không hợp lệ."));

        // Kiem tra lai kho ngay luc dat (kho co the doi tu luc xem gio).
        List<CartLine> selected = selectedLines(user, selectedVariantIds);
        if (selected.isEmpty()) {
            throw new IllegalArgumentException("Các sản phẩm đã chọn không còn trong giỏ hàng.");
        }
        for (CartLine line : selected) {
            ProductVariant v = line.card().getVariant();
            if (!line.available()) {
                throw new IllegalArgumentException("Sản phẩm " + line.card().name() + " đã hết hàng.");
            }
            if (v.getStockQuantity() < line.quantity()) {
                throw new IllegalArgumentException("Sản phẩm " + line.card().name()
                        + " chỉ còn " + v.getStockQuantity() + " sản phẩm trong kho.");
            }
        }

        // Voucher (neu co) phai hop le voi chinh cac dong duoc chon.
        Voucher applied = null;
        if (voucherCode != null && !voucherCode.isBlank()) {
            applied = cartService.findVoucherByCode(voucherCode.trim()).orElse(null);
            if (applied == null) {
                throw new IllegalArgumentException("Mã giảm giá không tồn tại.");
            }
            String reason = cartService.validateVoucher(applied, selected, LocalDateTime.now());
            if (reason != null) {
                throw new IllegalArgumentException(reason);
            }
        }

        // Tong tien: tam tinh - giam gia + phi ship - diem (1 diem = 1đ).
        CartService.Totals totals = cartService.totals(selected, applied);
        BigDecimal afterDiscount = totals.total().add(SHIPPING_FEE);
        int balance = user.getPointBalance() == null ? 0 : user.getPointBalance();
        int maxPoints = Math.min(balance, afterDiscount.intValue());
        int pointsUsed = Math.max(0, Math.min(usePoints, maxPoints));
        BigDecimal total = afterDiscount.subtract(BigDecimal.valueOf(pointsUsed));
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }

        // Gio giao ghi vao dau note (Orders khong co cot gio giao rieng).
        String fullNote = buildNote(deliverySlot, note);

        Orders order = new Orders();
        order.setUser(user);
        order.setOrderCode(newOrderCode());
        order.setOrderDate(LocalDateTime.now());
        order.setRecipientName(address.getRecipientName());
        order.setRecipientPhone(address.getPhone());
        order.setShippingAddress(address.fullAddress());
        order.setSubtotal(totals.subtotal());
        order.setShippingFee(SHIPPING_FEE);
        order.setDiscountAmount(totals.discount());
        order.setUsedPoints(pointsUsed);
        order.setTotalAmount(total);
        order.setStatus(OrderStatus.PENDING);
        order.setNote(fullNote);
        order = orders.save(order);

        for (CartLine line : selected) {
            ProductVariant v = line.card().getVariant();
            OrderDetail d = new OrderDetail();
            d.setOrderId(order.getId());
            d.setVariant(v);
            d.setQuantity(line.quantity());
            d.setUnitPrice(line.card().displayPrice() != null
                    ? line.card().displayPrice() : BigDecimal.ZERO);
            d.setTotalPrice(line.lineTotal());
            orderDetails.save(d);
            v.setStockQuantity(v.getStockQuantity() - line.quantity());
            variants.save(v);
        }

        Payment payment = new Payment();
        payment.setOrderId(order.getId());
        payment.setPaymentMethod(method);
        payment.setAmount(total);
        payment.setPaymentStatus(PaymentStatus.PENDING);
        payments.save(payment);

        if (applied != null) {
            OrderVoucher ov = new OrderVoucher();
            ov.setVoucher(applied);
            ov.setUser(user);
            ov.setOrder(order);
            ov.setDiscountAmount(totals.discount());
            orderVouchers.save(ov);
            applied.setUsedCount(applied.getUsedCount() + 1);
            vouchers.save(applied);
        }

        if (pointsUsed > 0) {
            int balanceAfter = balance - pointsUsed;
            user.setPointBalance(balanceAfter);
            users.save(user);
            PointTransaction pt = new PointTransaction();
            pt.setUser(user);
            pt.setOrderId(order.getId());
            pt.setType("SPEND");
            pt.setPoints(-pointsUsed);
            pt.setBalanceAfter(balanceAfter);
            pt.setDescription("Sử dụng điểm cho đơn hàng " + order.getOrderCode());
            pointTransactions.save(pt);
        }

        // Xoa cac mon da dat khoi gio hang.
        Cart cart = carts.findByUserId(user.getId()).orElse(null);
        if (cart != null) {
            for (CartLine line : selected) {
                cartItems.deleteByCartIdAndVariantId(cart.getId(), line.card().getVariant().getId());
            }
        }

        notifications.notify(user.getId(), "ORDER",
                "Đặt hàng thành công — " + order.getOrderCode(),
                "Shop sẽ xác nhận đơn hàng của bạn trong thời gian sớm nhất.",
                "/account/orders");

        return new PlacedOrder(order, selected, total);
    }

    private String buildNote(String deliverySlot, String note) {
        String slot = deliverySlot == null ? "" : deliverySlot.trim();
        String userNote = note == null ? "" : note.trim();
        if (!slot.isBlank() && !userNote.isBlank()) {
            return "Giao: " + slot + "; " + userNote;
        }
        if (!slot.isBlank()) {
            return "Giao: " + slot;
        }
        return userNote.isBlank() ? null : userNote;
    }

    /** Ma don duy nhat, chi gom chu + so de dung lam vnp_TxnRef. */
    private String newOrderCode() {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int rnd = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "LM" + ts + rnd;
    }
}
