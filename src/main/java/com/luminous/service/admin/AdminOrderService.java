package com.luminous.service.admin;

import com.luminous.dto.OrderCreateForm;
import com.luminous.dto.OrderLineItem;
import com.luminous.dto.OrderRow;
import com.luminous.dto.Vnd;
import com.luminous.model.OrderDetail;
import com.luminous.model.enums.OrderStatus;
import com.luminous.model.Orders;
import com.luminous.model.Payment;
import com.luminous.model.enums.PaymentMethod;
import com.luminous.model.enums.PaymentStatus;
import com.luminous.model.PointTransaction;
import com.luminous.model.ProductVariant;
import com.luminous.model.enums.SalesChannel;
import com.luminous.model.User;
import com.luminous.repository.UserRepository;
import com.luminous.repository.order.OrderDetailRepository;
import com.luminous.repository.order.OrderRepository;
import com.luminous.repository.order.PaymentRepository;
import com.luminous.repository.order.PointTransactionRepository;
import com.luminous.repository.product.ProductVariantRepository;
import com.luminous.service.ProductImage;
import com.luminous.service.notification.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.Predicate;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AdminOrderService {

    public record StatusOption(String value, String label) {
    }

    public record StepItem(String code, String label, String state, LocalDateTime time) {
    }

    private static final List<OrderStatus> STEPS = List.of(
            OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.SHIPPING,
            OrderStatus.DELIVERED, OrderStatus.COMPLETED);

    private final OrderRepository orders;
    private final OrderDetailRepository orderDetails;
    private final UserRepository users;
    private final PointTransactionRepository pointTransactions;
    private final ProductVariantRepository variants;
    private final PaymentRepository payments;
    private final NotificationService notifications;

    public AdminOrderService(OrderRepository orders,
                             OrderDetailRepository orderDetails,
                             UserRepository users,
                             PointTransactionRepository pointTransactions,
                             ProductVariantRepository variants,
                             PaymentRepository payments,
                             NotificationService notifications) {
        this.orders = orders;
        this.orderDetails = orderDetails;
        this.users = users;
        this.pointTransactions = pointTransactions;
        this.variants = variants;
        this.payments = payments;
        this.notifications = notifications;
    }

    public static String statusLabel(OrderStatus status) {
        return status == null ? null : status.label();
    }

    public static List<OrderStatus> nextStatuses(OrderStatus status) {
        return status == null ? List.of() : status.next();
    }

    public static List<StatusOption> nextStatusOptions(OrderStatus status) {
        return nextStatuses(status).stream()
                .map(code -> new StatusOption(code.name(), statusLabel(code)))
                .toList();
    }

    public static List<StepItem> stepItems(Orders order) {
        int current = STEPS.indexOf(order.getStatus());
        // Don da hoan thanh: buoc cuoi cung cung da xong nen ve tron xanh dac
        // (done). Neu de o dang "current" thi no ve vong tron trang vien xanh
        // (chi dung de danh dau "dang o buoc nay" o don chua xong).
        boolean allDone = current == STEPS.size() - 1;
        List<LocalDateTime> times = Arrays.asList(
                order.getOrderDate(), order.getConfirmedAt(), order.getShippedAt(),
                order.getDeliveredAt(), order.getCompletedAt());
        List<StepItem> items = new ArrayList<>();
        for (int i = 0; i < STEPS.size(); i++) {
            String state = current < 0 ? "todo"
                    : (i < current || (allDone && i == current) ? "done"
                    : (i == current ? "current" : "todo"));
            items.add(new StepItem(STEPS.get(i).name(), STEPS.get(i).label(), state, times.get(i)));
        }
        return items;
    }

    public Payment paymentOf(Integer orderId) {
        return payments.findTopByOrderIdOrderByIdDesc(orderId).orElse(null);
    }

    public String createdByName(Integer adminId) {
        if (adminId == null) {
            return null;
        }
        return users.findById(adminId).map(User::getFullName).orElse(null);
    }

    public String paymentMethodLabel(Integer orderId) {
        Payment payment = paymentOf(orderId);
        if (payment == null || payment.getPaymentMethod() == null) {
            return null;
        }
        return payment.getPaymentMethod().label();
    }

    public String saveBill(Integer orderId) throws IOException {
        Orders order = detail(orderId);
        List<OrderLineItem> items = lines(orderId);
        Payment payment = paymentOf(orderId);
        Path dir = Path.of("bill");
        Files.createDirectories(dir);
        String fileName = order.getOrderCode() + ".html";
        Files.writeString(dir.resolve(fileName),
                billHtml(order, items, payment, paymentMethodLabel(orderId),
                        createdByName(order.getCreatedBy())),
                StandardCharsets.UTF_8);
        return "bill/" + fileName;
    }

    private String billHtml(Orders order, List<OrderLineItem> items, Payment payment,
                            String method, String staff) {
        StringBuilder h = new StringBuilder();
        h.append("<!DOCTYPE html><html lang=\"vi\"><head><meta charset=\"UTF-8\">");
        h.append("<title>Hóa đơn ").append(esc(order.getOrderCode())).append("</title><style>");
        h.append("body{font-family:Arial,sans-serif;font-size:13px;color:#000;margin:0;padding:10px}");
        h.append(".c{text-align:center}.shop{font-size:18px;font-weight:800;letter-spacing:2px}");
        h.append(".title{font-size:15px;font-weight:800;margin-top:6px}");
        h.append(".row{display:flex;justify-content:space-between;gap:8px}");
        h.append(".sep{border-top:1px dashed #000;margin:8px 0}");
        h.append("table{width:100%;border-collapse:collapse}");
        h.append("th,td{padding:3px 4px;vertical-align:top;text-align:right}");
        h.append("th:first-child,td:first-child{text-align:left}");
        h.append("th{border-bottom:1px dashed #000}.sub{font-size:11px;color:#555}");
        h.append(".total{font-weight:800;font-size:14px}</style></head><body>");
        h.append("<div class=\"c\"><div class=\"shop\">LUMINOUS</div>");
        h.append("<div>Địa chỉ: 12 Nguyễn Văn Bảo, Gò Vấp, TP.HCM</div>");
        h.append("<div>SĐT: 0972606925</div>");
        h.append("<div class=\"title\">HÓA ĐƠN BÁN HÀNG</div></div>");
        h.append("<div class=\"row\"><span>Ngày: ")
                .append(order.getOrderDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .append("</span><span>Số phiếu: ").append(esc(order.getOrderCode())).append("</span></div>");
        h.append("<div class=\"row\"><span>Thu ngân: ").append(esc(staff == null ? "—" : staff))
                .append("</span><span>In lúc: ")
                .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")))
                .append("</span></div>");
        h.append("<div class=\"sep\"></div>");
        h.append("<table><thead><tr><th>Mặt hàng</th><th>SL</th><th>Đơn giá</th><th>T.tiền</th></tr></thead><tbody>");
        for (OrderLineItem line : items) {
            h.append("<tr><td><div>").append(esc(line.productName())).append("</div>");
            h.append("<div class=\"sub\">").append(esc(line.variantLabel())).append("</div></td>");
            h.append("<td>").append(line.quantity()).append("</td>");
            h.append("<td>").append(line.unitPriceText()).append("</td>");
            h.append("<td>").append(line.totalPriceText()).append("</td></tr>");
        }
        h.append("</tbody></table><div class=\"sep\"></div>");
        h.append("<div class=\"row\"><span>Tiền hàng</span><span>")
                .append(Vnd.format(order.getSubtotal())).append("</span></div>");
        h.append("<div class=\"row\"><span>Chiết khấu</span><span>-")
                .append(Vnd.format(order.getDiscountAmount())).append("</span></div>");
        h.append("<div class=\"row\"><span>Phí vận chuyển</span><span>")
                .append(Vnd.format(order.getShippingFee())).append("</span></div>");
        h.append("<div class=\"row total\"><span>Tổng cộng</span><span>")
                .append(Vnd.format(order.getTotalAmount())).append("</span></div>");
        if (payment != null && payment.getCashGiven() != null) {
            h.append("<div class=\"row\"><span>Khách đưa</span><span>")
                    .append(Vnd.format(payment.getCashGiven())).append("</span></div>");
        }
        if (payment != null && payment.getChangeAmount() != null) {
            h.append("<div class=\"row\"><span>Trả lại</span><span>")
                    .append(Vnd.format(payment.getChangeAmount())).append("</span></div>");
        }
        h.append("<div class=\"row\"><span>Thanh toán</span><span>")
                .append(esc(method == null ? "—" : method)).append("</span></div>");
        h.append("<div class=\"sep\"></div><div class=\"c\">Xin cảm ơn Quý khách!</div>");
        h.append("</body></html>");
        return h.toString();
    }

    private static String esc(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    public Page<OrderRow> page(String status, String q, LocalDate from, LocalDate to, String channel, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
        LocalDateTime fromAt = from == null ? null : from.atStartOfDay();
        LocalDateTime toAt = to == null ? null : to.plusDays(1).atStartOfDay();
        Page<Orders> result = orders.findAll(spec(status, q, fromAt, toAt, channel), pageable);
        return result.map(this::toRow);
    }

    private Specification<Orders> spec(String status, String q, LocalDateTime from, LocalDateTime to, String channel) {
        return (root, query, cb) -> {
            List<Predicate> and = new ArrayList<>();
            // Chi hien don chua bi admin an (an chi an khoi man hinh,
            // don van con trong DB, van mo duoc qua link chi tiet).
            and.add(cb.or(cb.isFalse(root.get("adminHidden")), cb.isNull(root.get("adminHidden"))));
            OrderStatus filter = OrderStatus.from(status);
            if (filter != null) {
                and.add(cb.equal(root.get("status"), filter));
            }
            SalesChannel channelFilter = SalesChannel.from(channel);
            if (channelFilter != null) {
                and.add(cb.equal(root.get("salesChannel"), channelFilter));
            }
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                and.add(cb.or(
                        cb.like(cb.lower(root.get("orderCode")), like),
                        cb.like(cb.lower(root.get("recipientName")), like),
                        cb.like(cb.lower(root.get("recipientPhone")), like)));
            }
            if (from != null) {
                and.add(cb.greaterThanOrEqualTo(root.get("orderDate"), from));
            }
            if (to != null) {
                and.add(cb.lessThan(root.get("orderDate"), to));
            }
            return and.isEmpty() ? null : cb.and(and.toArray(Predicate[]::new));
        };
    }

    @Transactional
    public Integer createOrder(OrderCreateForm form, Integer adminId) {
        User user = null;
        if (form.isWalkIn()) {
            if (form.getWalkInName() == null || form.getWalkInName().isBlank()) {
                throw new IllegalArgumentException("Vui lòng nhập tên khách vãng lai");
            }
        } else {
            if (form.getCustomerId() == null) {
                throw new IllegalArgumentException("Chưa chọn khách hàng");
            }
            user = users.findById(form.getCustomerId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng"));
        }
        if (form.getVariantIds() == null || form.getVariantIds().isEmpty()) {
            throw new IllegalArgumentException("Chưa chọn sản phẩm nào cho đơn hàng");
        }
        List<OrderDetail> details = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (int i = 0; i < form.getVariantIds().size(); i++) {
            Integer variantId = form.getVariantIds().get(i);
            int quantity = form.getQuantities() == null || i >= form.getQuantities().size()
                    || form.getQuantities().get(i) == null ? 1 : form.getQuantities().get(i);
            if (quantity < 1) {
                throw new IllegalArgumentException("Số lượng phải từ 1 trở lên");
            }
            ProductVariant variant = variants.findById(variantId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy biến thể sản phẩm"));
            int stock = variant.getStockQuantity() == null ? 0 : variant.getStockQuantity();
            if (stock < quantity) {
                throw new IllegalArgumentException("Sản phẩm " + variant.getProduct().getName() + " chỉ còn " + stock);
            }
            BigDecimal unit = variant.getSalePrice() != null ? variant.getSalePrice() : variant.getPrice();
            OrderDetail detail = new OrderDetail();
            detail.setVariant(variant);
            detail.setQuantity(quantity);
            detail.setUnitPrice(unit);
            detail.setTotalPrice(unit.multiply(BigDecimal.valueOf(quantity)));
            details.add(detail);
            subtotal = subtotal.add(detail.getTotalPrice());
            variant.setStockQuantity(stock - quantity);
            variants.save(variant);
        }
        SalesChannel channel = SalesChannel.from(form.getSalesChannel());
        if (channel == null) {
            channel = SalesChannel.ONLINE;
        }
        boolean store = channel == SalesChannel.STORE;
        PaymentMethod method = PaymentMethod.from(form.getPaymentMethod());
        if (method == null) {
            method = PaymentMethod.COD;
        }
        BigDecimal shipping = form.getShippingFee() == null ? BigDecimal.valueOf(30000) : form.getShippingFee();
        BigDecimal discount = form.getDiscountAmount() == null ? BigDecimal.ZERO : form.getDiscountAmount();
        String address = form.getShippingAddress();
        String recipientName = form.getRecipientName();
        String recipientPhone = form.getRecipientPhone();
        String walkInName = form.getWalkInName() == null ? null : form.getWalkInName().trim();
        String walkInPhone = form.getWalkInPhone() == null ? null : form.getWalkInPhone().trim();
        if (recipientName == null || recipientName.isBlank()) {
            recipientName = form.isWalkIn() ? walkInName
                    : (user == null ? null : user.getFullName());
        }
        if (recipientPhone == null || recipientPhone.isBlank()) {
            recipientPhone = form.isWalkIn() ? walkInPhone
                    : (user == null ? null : user.getPhone());
        }
        if (store) {
            shipping = BigDecimal.ZERO;
            if (address == null || address.isBlank()) {
                address = "12 Nguyễn Văn Bảo, Gò Vấp, TP.HCM";
            }
        } else if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập địa chỉ giao hàng");
        }
        BigDecimal total = subtotal.add(shipping).subtract(discount).max(BigDecimal.ZERO);
        if (method == PaymentMethod.COD && form.getCashGiven() != null
                && form.getCashGiven().compareTo(total) < 0) {
            throw new IllegalArgumentException("Tiền khách đưa phải đủ tổng tiền");
        }
        Orders order = new Orders();
        order.setUser(user);
        order.setOrderCode("DH-TAM");
        order.setOrderDate(LocalDateTime.now());
        order.setSalesChannel(channel);
        order.setCreatedBy(adminId);
        order.setRecipientName(recipientName);
        order.setRecipientPhone(recipientPhone);
        order.setShippingAddress(address);
        order.setSubtotal(subtotal);
        order.setShippingFee(shipping);
        order.setDiscountAmount(discount);
        order.setUsedPoints(0);
        order.setTotalAmount(total);
        order.setStatus(store ? OrderStatus.COMPLETED : OrderStatus.PENDING);
        order.setNote(blankToNull(form.getNote()));
        order.setUpdatedAt(LocalDateTime.now());
        if (store) {
            order.setCompletedAt(LocalDateTime.now());
        }
        orders.save(order);
        order.setOrderCode("DH" + String.format("%06d", order.getId()));
        orders.save(order);
        for (OrderDetail detail : details) {
            detail.setOrderId(order.getId());
            orderDetails.save(detail);
        }
        Payment payment = new Payment();
        payment.setOrderId(order.getId());
        payment.setPaymentMethod(method);
        payment.setAmount(total);
        payment.setPaymentStatus(store ? PaymentStatus.PAID : PaymentStatus.PENDING);
        payment.setPaidAt(store ? LocalDateTime.now() : null);
        if (method == PaymentMethod.COD && form.getCashGiven() != null) {
            payment.setCashGiven(form.getCashGiven());
            payment.setChangeAmount(form.getCashGiven().subtract(total));
        }
        payments.save(payment);
        if (store) {
            awardPoints(order);
        }
        return order.getId();
    }

    @Transactional
    public int[] bulkChangeStatus(List<Integer> ids, String newStatus) {
        int updated = 0;
        int skipped = 0;
        OrderStatus target = OrderStatus.from(newStatus);
        for (Integer id : ids) {
            Orders order = orders.findById(id).orElse(null);
            if (order == null || target == null || !nextStatuses(order.getStatus()).contains(target)) {
                skipped++;
                continue;
            }
            changeStatus(id, target,
                    target == OrderStatus.CANCELLED ? "Đã hủy bởi quản trị viên" : null);
            updated++;
        }
        return new int[]{updated, skipped};
    }

    // An hang loat don khoi danh sach admin (khong xoa khoi DB).
    @Transactional
    public int bulkHide(List<Integer> ids) {
        int done = 0;
        for (Integer id : ids) {
            Orders order = orders.findById(id).orElse(null);
            if (order == null || Boolean.TRUE.equals(order.getAdminHidden())) {
                continue;
            }
            order.setAdminHidden(true);
            orders.save(order);
            done++;
        }
        return done;
    }

    public List<User> customers() {
        return users.findAll(Sort.by("fullName").ascending()).stream()
                .filter(user -> user.getRole() != null && "ROLE_CUSTOMER".equals(user.getRole().getName()))
                .toList();
    }

    public record VariantOption(Integer id, String label, BigDecimal price, Integer stock) {
    }

    public List<VariantOption> activeVariants() {
        List<ProductVariant> all = variants.findAll();
        all.sort((a, b) -> a.getProduct().getName().compareToIgnoreCase(b.getProduct().getName()));
        List<VariantOption> result = new ArrayList<>();
        for (ProductVariant variant : all) {
            if (!Boolean.TRUE.equals(variant.getIsActive())
                    || variant.getStockQuantity() == null || variant.getStockQuantity() <= 0) {
                continue;
            }
            String label = variant.getProduct().getName();
            String detail = variant.getSize() != null ? variant.getSize() : variant.getSku();
            if (variant.getShadeName() != null && !variant.getShadeName().isBlank()) {
                detail = detail + " - " + variant.getShadeName();
            }
            BigDecimal price = variant.getSalePrice() != null ? variant.getSalePrice() : variant.getPrice();
            label = label + " - " + detail;
            result.add(new VariantOption(variant.getId(), label, price, variant.getStockQuantity()));
        }
        return result;
    }

    public Orders detail(Integer id) {
        Orders order = orders.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));
        if (order.getUser() != null) {
            order.getUser().getFullName();
        }
        return order;
    }

    public List<OrderLineItem> lines(Integer orderId) {
        return orderDetails.findByOrderIdOrderByIdAsc(orderId).stream()
                .map(detail -> {
                    var variant = detail.getVariant();
                    var product = variant.getProduct();
                    String label = variant.getSize() != null ? variant.getSize() : variant.getSku();
                    if (variant.getShadeName() != null && !variant.getShadeName().isBlank()) {
                        label = label + " - " + variant.getShadeName();
                    }
                    return new OrderLineItem(product.getName(), label, ProductImage.imageFor(product),
                            detail.getQuantity(), detail.getUnitPrice(), detail.getTotalPrice());
                })
                .toList();
    }

    @Transactional
    public void changeStatus(Integer id, String newStatus) {
        changeStatus(id, newStatus, null);
    }

    /**
     * Hoan thanh don: CHI dung cho khach xac nhan da nhan hang, job tu dong
     * hoan thanh sau 3 ngay, va don tai quay tao san o trang thai hoan thanh.
     * Admin KHONG goi duoc ham nay tu trang chi tiet don (changeStatus da
     * chan DELIVERED -> COMPLETED). Cong diem 1 diem/1.000d nhu truoc.
     */
    @Transactional
    public void completeOrder(Integer id) {
        Orders order = orders.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new IllegalArgumentException("Chỉ hoàn thành được đơn đang ở trạng thái đã giao");
        }
        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orders.save(order);
        awardPoints(order);
        if (order.getUser() != null) {
            notifications.notify(order.getUser().getId(), "ORDER",
                    "Đơn hàng " + order.getOrderCode() + " đã hoàn thành",
                    "Cảm ơn bạn đã mua sắm tại Luminous. Bạn có 3 ngày để đánh giá sản phẩm.",
                    "/account/orders");
        }
    }

    @Transactional
    public void changeStatus(Integer id, String newStatus, String cancelReason) {
        OrderStatus target = OrderStatus.from(newStatus);
        if (target == null) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ");
        }
        changeStatus(id, target, cancelReason);
    }

    @Transactional
    public void changeStatus(Integer id, OrderStatus target, String cancelReason) {
        Orders order = orders.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));
        if (!nextStatuses(order.getStatus()).contains(target)) {
            throw new IllegalArgumentException("Không thể chuyển từ "
                    + statusLabel(order.getStatus()) + " sang " + statusLabel(target));
        }
        if (target == OrderStatus.CANCELLED) {
            if (cancelReason == null || cancelReason.isBlank()) {
                throw new IllegalArgumentException("Vui lòng nhập lý do hủy đơn hàng");
            }
            order.setCancelReason(cancelReason.trim());
        } else if (target == OrderStatus.RETURNED) {
            // Ly do tra hang: khach tra hang nhap o trang Don mua, admin
            // chuyen tay o trang chi tiet co the de trong.
            if (cancelReason != null && !cancelReason.isBlank()) {
                order.setReturnReason(cancelReason.trim());
            }
        } else if (order.getStatus() == OrderStatus.CANCELLED && target == OrderStatus.PENDING) {
            order.setCancelReason(null);
            order.setCancelledAt(null);
        }
        LocalDateTime now = LocalDateTime.now();
        switch (target) {
            case CONFIRMED -> order.setConfirmedAt(now);
            case SHIPPING -> order.setShippedAt(now);
            case DELIVERED -> order.setDeliveredAt(now);
            case COMPLETED -> order.setCompletedAt(now);
            case CANCELLED -> order.setCancelledAt(now);
            case RETURNED -> order.setReturnedAt(now);
            default -> {
            }
        }
        order.setStatus(target);
        order.setUpdatedAt(LocalDateTime.now());
        orders.save(order);
        if (target == OrderStatus.COMPLETED) {
            awardPoints(order);
        }
        if (order.getUser() != null) {
            String message = switch (target) {
                case CONFIRMED -> "Shop đã xác nhận đơn hàng của bạn.";
                case SHIPPING -> "Đơn hàng của bạn đang được giao.";
                case DELIVERED -> "Đơn hàng của bạn đã giao. Bạn có 3 ngày để xác nhận, trả hàng hoặc đánh giá.";
                case CANCELLED -> "Đơn hàng đã hủy"
                        + (order.getCancelReason() != null ? " (" + order.getCancelReason() + ")" : "") + ".";
                case RETURNED -> "Shop đã nhận yêu cầu trả hàng của bạn và sẽ liên hệ để nhận lại hàng, hoàn tiền.";
                default -> "Trạng thái đơn hàng đã cập nhật.";
            };
            notifications.notify(order.getUser().getId(), "ORDER",
                    "Đơn hàng " + order.getOrderCode() + " — " + statusLabel(target),
                    message, "/account/orders");
        }
    }

    private void awardPoints(Orders order) {
        if (order.getUser() == null) {
            return;
        }
        if (pointTransactions.existsByOrderIdAndType(order.getId(), "EARN")) {
            return;
        }
        int points = order.getTotalAmount().divideToIntegralValue(BigDecimal.valueOf(1000)).intValue();
        if (points <= 0) {
            return;
        }
        User user = users.findById(order.getUser().getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng"));
        int balance = (user.getPointBalance() == null ? 0 : user.getPointBalance()) + points;
        user.setPointBalance(balance);
        users.save(user);
        PointTransaction transaction = new PointTransaction();
        transaction.setUser(user);
        transaction.setOrderId(order.getId());
        transaction.setType("EARN");
        transaction.setPoints(points);
        transaction.setBalanceAfter(balance);
        transaction.setDescription("Tích điểm từ đơn hàng " + order.getOrderCode());
        pointTransactions.save(transaction);
        notifications.notify(user.getId(), "POINTS",
                "Bạn nhận được " + points + " điểm",
                "Tích điểm từ đơn hàng " + order.getOrderCode() + ". Số dư hiện tại: " + balance + " điểm.",
                "/account/points");
    }

    private OrderRow toRow(Orders order) {
        int items = (int) orderDetails.countByOrderId(order.getId());
        return new OrderRow(
                order.getId(),
                order.getOrderCode(),
                order.getUser() == null ? order.getRecipientName() : order.getUser().getFullName(),
                order.getRecipientPhone(),
                order.getOrderDate(),
                order.getTotalAmount(),
                order.getStatus(),
                statusLabel(order.getStatus()),
                items,
                order.getSalesChannel());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
