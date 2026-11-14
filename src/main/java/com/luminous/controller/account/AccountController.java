package com.luminous.controller.account;

import com.luminous.dto.OrderLineItem;
import com.luminous.dto.PasswordForm;
import com.luminous.dto.ProfileForm;
import com.luminous.dto.ReturnForm;
import com.luminous.dto.ReviewBatchForm;
import com.luminous.dto.RoutineStep;
import com.luminous.model.Notification;
import com.luminous.model.OrderDetail;
import com.luminous.model.Orders;
import com.luminous.model.Review;
import com.luminous.model.SkinQuizResult;
import com.luminous.model.SkinRoutine;
import com.luminous.model.User;
import com.luminous.model.Voucher;
import com.luminous.model.enums.OrderStatus;
import com.luminous.repository.UserRepository;
import com.luminous.repository.order.OrderDetailRepository;
import com.luminous.repository.order.OrderRepository;
import com.luminous.repository.order.PointTransactionRepository;
import com.luminous.repository.review.ReviewMediaRepository;
import com.luminous.repository.review.ReviewRepository;
import com.luminous.repository.skinquiz.SkinQuizResultRepository;
import com.luminous.repository.skinquiz.SkinRoutineRepository;
import com.luminous.repository.voucher.VoucherRepository;
import com.luminous.security.CustomUserDetails;
import com.luminous.service.FileStorageService;
import com.luminous.service.ProductImage;
import com.luminous.service.account.CustomerReviewService;
import com.luminous.service.admin.AdminOrderService;
import com.luminous.service.admin.OrderAutoCompleteService;
import com.luminous.service.auth.VerificationService;
import com.luminous.service.notification.NotificationService;
import com.luminous.service.shop.CartService;
import com.luminous.service.skinquiz.SkinQuizService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.text.NumberFormat;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Trang ca nhan cua khach (sidebar giong hinh mau, KHONG co muc sale):
 * Ho so, Ket qua trac nghiem, Don mua (7 tab loc), Doi mat khau.
 * Thong bao / Kho Voucher / Shopee Xu chua co chuc nang that nen tam
 * dung trang coming-soon co san, khong lam nut gia.
 */
@Controller
@RequestMapping("/account")
public class AccountController {

    /** Tab loc don mua (ten tab theo yeu cau, anh xa sang trang thai don that trong DB). */
    public record OrderTab(String key, String label, List<OrderStatus> statuses) {
    }

    public static final List<OrderTab> TABS = List.of(
            new OrderTab("ALL", "Tất cả", null),
            new OrderTab("PENDING_PAYMENT", "Chờ thanh toán", List.of(OrderStatus.PENDING)),
            new OrderTab("SHIPPING", "Vận chuyển", List.of(OrderStatus.CONFIRMED, OrderStatus.SHIPPING)),
            new OrderTab("AWAITING_DELIVERY", "Chờ giao hàng", List.of(OrderStatus.DELIVERED)),
            new OrderTab("COMPLETED", "Hoàn thành", List.of(OrderStatus.COMPLETED)),
            new OrderTab("CANCELLED", "Đã hủy", List.of(OrderStatus.CANCELLED)),
            new OrderTab("RETURNED", "Trả hàng", List.of(OrderStatus.RETURNED)));

    private static OrderTab tabOf(String key) {
        return TABS.stream().filter(t -> t.key().equals(key)).findFirst().orElse(TABS.get(0));
    }

    public record OrderCard(Orders order, List<OrderLineItem> lines,
                            LocalDateTime reviewDeadline,
                            boolean canReview, boolean canReturn,
                            boolean canConfirmReceived, boolean allReviewed) {
    }

    /** Ly do tra hang cho form yeu cau tra hang / hoan tien. */
    public static final List<String> RETURN_REASONS = List.of(
            "Sản phẩm bị lỗi / hỏng",
            "Giao sai sản phẩm",
            "Sản phẩm không giống mô tả",
            "Sản phẩm hết hạn / gần hết hạn",
            "Đổi ý, không còn nhu cầu",
            "Lý do khác");

    public record RefundOption(String value, String label) {
    }

    public static final List<RefundOption> REFUND_METHODS = List.of(
            new RefundOption("ORIGINAL", "Hoàn tiền về phương thức đã thanh toán"),
            new RefundOption("BANK", "Chuyển khoản ngân hàng"));

    private final UserRepository users;
    private final OrderRepository orders;
    private final OrderDetailRepository orderDetails;
    private final ReviewRepository reviews;
    private final ReviewMediaRepository reviewMedia;
    private final CustomerReviewService reviewService;
    private final AdminOrderService adminOrders;
    private final NotificationService notifications;
    private final VoucherRepository vouchers;
    private final PointTransactionRepository pointTransactions;
    private final VerificationService verification;
    private final CartService cartService;
    private final FileStorageService fileStorage;
    private final SkinQuizService skinService;
    private final SkinQuizResultRepository quizResults;
    private final SkinRoutineRepository routines;
    private final PasswordEncoder passwords;

    public AccountController(UserRepository users,
                             OrderRepository orders,
                             OrderDetailRepository orderDetails,
                             ReviewRepository reviews,
                             ReviewMediaRepository reviewMedia,
                             CustomerReviewService reviewService,
                             AdminOrderService adminOrders,
                             NotificationService notifications,
                             VoucherRepository vouchers,
                             PointTransactionRepository pointTransactions,
                             VerificationService verification,
                             CartService cartService,
                             FileStorageService fileStorage,
                             SkinQuizService skinService,
                             SkinQuizResultRepository quizResults,
                             SkinRoutineRepository routines,
                             PasswordEncoder passwords) {
        this.users = users;
        this.orders = orders;
        this.orderDetails = orderDetails;
        this.reviews = reviews;
        this.reviewMedia = reviewMedia;
        this.reviewService = reviewService;
        this.adminOrders = adminOrders;
        this.notifications = notifications;
        this.vouchers = vouchers;
        this.pointTransactions = pointTransactions;
        this.verification = verification;
        this.cartService = cartService;
        this.fileStorage = fileStorage;
        this.skinService = skinService;
        this.quizResults = quizResults;
        this.routines = routines;
        this.passwords = passwords;
    }

    private User currentUser(CustomUserDetails details) {
        return users.findById(details.getUser().getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản"));
    }

    @GetMapping
    public String account() {
        return "redirect:/account/profile";
    }

    // ---------- Ho so ----------

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal CustomUserDetails details, Model model) {
        User user = currentUser(details);
        if (!model.containsAttribute("profileForm")) {
            ProfileForm form = new ProfileForm();
            form.setFullName(user.getFullName());
            form.setPhone(user.getPhone());
            form.setGender(user.getGender() == null ? "" : user.getGender());
            if (user.getDateOfBirth() != null) {
                form.setBirthDay(String.valueOf(user.getDateOfBirth().getDayOfMonth()));
                form.setBirthMonth(String.valueOf(user.getDateOfBirth().getMonthValue()));
                form.setBirthYear(String.valueOf(user.getDateOfBirth().getYear()));
            }
            model.addAttribute("profileForm", form);
        }
        model.addAttribute("user", user);
        model.addAttribute("accountMenu", "profile");
        return "account/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@Valid @ModelAttribute("profileForm") ProfileForm form,
                                BindingResult result,
                                @AuthenticationPrincipal CustomUserDetails details,
                                Model model,
                                RedirectAttributes redirect) {
        User user = currentUser(details);
        // Ngay sinh: de trong ca 3 o = giu nguyen; nhap 1 o thi phai nhap du 3 o va dung ngay.
        boolean anyDob = (form.getBirthDay() != null && !form.getBirthDay().isBlank())
                || (form.getBirthMonth() != null && !form.getBirthMonth().isBlank())
                || (form.getBirthYear() != null && !form.getBirthYear().isBlank());
        LocalDate dob = user.getDateOfBirth();
        if (anyDob && !result.hasErrors()) {
            if (form.getBirthDay() == null || form.getBirthDay().isBlank()
                    || form.getBirthMonth() == null || form.getBirthMonth().isBlank()
                    || form.getBirthYear() == null || form.getBirthYear().isBlank()) {
                result.rejectValue("birthDay", "dob.incomplete", "Vui lòng chọn đủ ngày, tháng, năm sinh");
            } else {
                try {
                    dob = LocalDate.of(Integer.parseInt(form.getBirthYear()),
                            Integer.parseInt(form.getBirthMonth()),
                            Integer.parseInt(form.getBirthDay()));
                    if (dob.isAfter(LocalDate.now())) {
                        result.rejectValue("birthDay", "dob.future", "Ngày sinh không được ở tương lai");
                    }
                } catch (DateTimeException | NumberFormatException e) {
                    result.rejectValue("birthDay", "dob.invalid", "Ngày sinh không hợp lệ (ví dụ: tháng 2 không có ngày 30)");
                }
            }
        }
        if (result.hasErrors()) {
            model.addAttribute("user", user);
            model.addAttribute("accountMenu", "profile");
            return "account/profile";
        }
        // So dien thoai KHONG luu tu form Ho so: doi sdt qua muc rieng
        // "Doi so dien thoai" (xac thuc bang ma OTP gui toi so moi).
        user.setFullName(form.getFullName().trim());
        user.setGender(form.getGender() == null || form.getGender().isBlank() ? null : form.getGender());
        user.setDateOfBirth(dob);
        user.setUpdatedAt(LocalDateTime.now());
        users.save(user);
        redirect.addFlashAttribute("saved", true);
        return "redirect:/account/profile";
    }

    /** Tai anh dai dien: luu vao uploads/avatars, hien qua /uploads/** co san. */
    @PostMapping("/profile/avatar")
    public String updateAvatar(@RequestParam("avatar") MultipartFile avatar,
                               @AuthenticationPrincipal CustomUserDetails details,
                               RedirectAttributes redirect) {
        User user = currentUser(details);
        try {
            String url = fileStorage.store(avatar, "avatars", "user-" + user.getId());
            if (url == null) {
                redirect.addFlashAttribute("avatarError", "Vui lòng chọn một ảnh để tải lên");
                return "redirect:/account/profile";
            }
            user.setAvtUrl(url);
            user.setUpdatedAt(LocalDateTime.now());
            users.save(user);
            redirect.addFlashAttribute("saved", true);
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("avatarError", e.getMessage());
        }
        return "redirect:/account/profile";
    }

    // ---------- Doi so dien thoai (xac thuc bang ma OTP gui toi so moi) ----------

    /** Gui ma 6 so toi so dien thoai moi (tra ve JSON cho nut "Gui ma" o trang Ho so). */
    @PostMapping("/profile/phone/send-code")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendPhoneChangeCode(
            @RequestParam String newPhone,
            @AuthenticationPrincipal CustomUserDetails details) {
        String phone = newPhone == null ? "" : newPhone.trim();
        if (!phone.matches("^0\\d{9}$")) {
            return ResponseEntity.ok(Map.of("ok", false,
                    "error", "Số điện thoại phải bắt đầu bằng 0 và đủ 10 số"));
        }
        User user = currentUser(details);
        if (phone.equals(user.getPhone())) {
            return ResponseEntity.ok(Map.of("ok", false, "error", "Đây đã là số điện thoại hiện tại của bạn"));
        }
        if (users.existsByPhone(phone)) {
            return ResponseEntity.ok(Map.of("ok", false, "error", "Số điện thoại này đã được đăng ký"));
        }
        VerificationService.SendResult result =
                verification.sendCode(phone, VerificationService.Purpose.CHANGE_PHONE);
        if (!result.ok()) {
            return ResponseEntity.ok(Map.of("ok", false, "error", result.message()));
        }
        return ResponseEntity.ok(Map.of("ok", true, "message", result.message()));
    }

    /** Xac nhan doi so: nhap dung ma OTP thi doi sdt (email ao phone_... doi theo). */
    @PostMapping("/profile/phone")
    public String changePhone(@RequestParam String newPhone,
                              @RequestParam String code,
                              @AuthenticationPrincipal CustomUserDetails details,
                              RedirectAttributes redirect) {
        String phone = newPhone == null ? "" : newPhone.trim();
        User user = currentUser(details);
        if (!phone.matches("^0\\d{9}$")) {
            redirect.addFlashAttribute("phoneError", "Số điện thoại phải bắt đầu bằng 0 và đủ 10 số");
            return "redirect:/account/profile";
        }
        if (phone.equals(user.getPhone())) {
            redirect.addFlashAttribute("phoneError", "Đây đã là số điện thoại hiện tại của bạn");
            return "redirect:/account/profile";
        }
        if (users.existsByPhone(phone)) {
            redirect.addFlashAttribute("phoneError", "Số điện thoại này đã được đăng ký");
            return "redirect:/account/profile";
        }
        if (!verification.verify(phone, code, VerificationService.Purpose.CHANGE_PHONE)) {
            redirect.addFlashAttribute("phoneError", "Mã xác thực không đúng hoặc đã hết hạn, vui lòng gửi lại mã mới");
            return "redirect:/account/profile";
        }
        String oldPhone = user.getPhone();
        user.setPhone(phone);
        // Tai khoan dang ky bang sdt co email ao phone_<sdt cu>@luminous.vn: doi theo sdt moi.
        if (oldPhone != null && ("phone_" + oldPhone + "@luminous.vn").equals(user.getEmail())) {
            user.setEmail("phone_" + phone + "@luminous.vn");
        }
        user.setUpdatedAt(LocalDateTime.now());
        users.save(user);
        redirect.addFlashAttribute("phoneChanged", phone);
        return "redirect:/account/profile";
    }

    // ---------- Doi mat khau ----------

    @GetMapping("/change-password")
    public String changePasswordForm(Model model) {
        if (!model.containsAttribute("passwordForm")) {
            model.addAttribute("passwordForm", new PasswordForm());
        }
        model.addAttribute("accountMenu", "change-password");
        return "account/change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(@Valid @ModelAttribute("passwordForm") PasswordForm form,
                                 BindingResult result,
                                 @AuthenticationPrincipal CustomUserDetails details,
                                 Model model,
                                 RedirectAttributes redirect) {
        User user = currentUser(details);
        if (!result.hasErrors() && !passwords.matches(form.getCurrentPassword(), user.getPassword())) {
            result.rejectValue("currentPassword", "mismatch", "Mật khẩu hiện tại không đúng");
        }
        if (!result.hasErrors() && !form.getNewPassword().equals(form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "mismatch", "Mật khẩu nhập lại không khớp");
        }
        if (result.hasErrors()) {
            model.addAttribute("accountMenu", "change-password");
            return "account/change-password";
        }
        user.setPassword(passwords.encode(form.getNewPassword()));
        users.save(user);
        redirect.addFlashAttribute("changed", true);
        return "redirect:/account/change-password";
    }

    // ---------- Ket qua trac nghiem ----------

    @GetMapping("/skin-result")
    @Transactional(readOnly = true)
    public String skinResult(@AuthenticationPrincipal CustomUserDetails details, Model model) {
        User user = currentUser(details);
        SkinQuizResult result = quizResults.findFirstByUserIdOrderByCompletedAtDesc(user.getId()).orElse(null);
        SkinRoutine routine = routines.findByUserId(user.getId()).orElse(null);
        model.addAttribute("user", user);
        model.addAttribute("result", result);
        model.addAttribute("routine", routine);
        model.addAttribute("accountMenu", "skin-result");
        if (routine != null) {
            model.addAttribute("skin", skinService.skinInfo(routine.getSkinType()));
            model.addAttribute("goalLabels", skinService.goalLabels(
                    routine.getGoals() == null || routine.getGoals().isBlank()
                            ? List.of() : List.of(routine.getGoals().split(","))));
            model.addAttribute("budgetLabel", skinService.budgetLabel(routine.getBudget()));
            List<RoutineStep> steps = skinService.loadSteps(routine);
            model.addAttribute("stepCount", steps.size());
            model.addAttribute("totalPriceText", NumberFormat.getInstance(new Locale("vi", "VN"))
                    .format(skinService.totalPrice(steps)) + "đ");
        } else if (result != null) {
            model.addAttribute("skin", skinService.skinInfo(result.getSkinType()));
        }
        return "account/skin-result";
    }

    // ---------- Don mua ----------

    @GetMapping("/orders")
    @Transactional(readOnly = true)
    public String myOrders(@RequestParam(defaultValue = "ALL") String tab,
                           @AuthenticationPrincipal CustomUserDetails details,
                           Model model) {
        User user = currentUser(details);
        OrderTab activeTab = tabOf(tab);
        List<Orders> all = orders.findByUserIdOrderByOrderDateDesc(user.getId());
        Map<String, Long> counts = new LinkedHashMap<>();
        for (OrderTab t : TABS) {
            counts.put(t.key(), t.statuses() == null ? (long) all.size()
                    : all.stream().filter(o -> t.statuses().contains(o.getStatus())).count());
        }
        List<Orders> filtered = activeTab.statuses() == null ? all
                : all.stream().filter(o -> activeTab.statuses().contains(o.getStatus())).toList();
        List<OrderCard> cards = new ArrayList<>();
        for (Orders order : filtered) {
            cards.add(cardOf(order, user.getId()));
        }
        model.addAttribute("user", user);
        model.addAttribute("tabs", TABS);
        model.addAttribute("activeTab", activeTab);
        model.addAttribute("tabCounts", counts);
        model.addAttribute("orderCards", cards);
        model.addAttribute("accountMenu", "orders");
        return "account/orders";
    }

    /** Chi tiet 1 don: bam vao ma don (hoac "Xem chi tiet") o trang Don mua. */
    @GetMapping("/orders/{id}")
    @Transactional(readOnly = true)
    public String orderDetail(@PathVariable Integer id,
                              @AuthenticationPrincipal CustomUserDetails details,
                              Model model) {
        Orders order = ownOrder(id, details);
        User user = currentUser(details);
        model.addAttribute("user", user);
        model.addAttribute("card", cardOf(order, user.getId()));
        // Thanh trang thai 5 buoc giong trang chi tiet don ben admin
        // (AdminOrderService.stepItems): label + moc thoi gian + trang thai
        // done/current/todo cua tung buoc. Nhan vien tao: don online thuong
        // khong co (hien "—"), don tai quay co ten admin tao.
        model.addAttribute("stepItems", AdminOrderService.stepItems(order));
        model.addAttribute("createdByName", adminOrders.createdByName(order.getCreatedBy()));
        model.addAttribute("accountMenu", "orders");
        return "account/order-detail";
    }

    /**
     * Co nut gi tren the don: han 3 ngay tinh tu ngay giao (don tai quay:
     * ngay hoan thanh). Trong han thi duoc danh gia / tra hang / xac nhan
     * da nhan; het han thi don tu hoan thanh, khong danh gia / tra hang nua.
     */
    private OrderCard cardOf(Orders order, Integer userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = OrderAutoCompleteService.reviewDeadline(order);
        boolean withinWindow = OrderAutoCompleteService.isWithinReviewWindow(order, now);
        boolean deliveredOrCompleted = order.getStatus() == OrderStatus.DELIVERED
                || order.getStatus() == OrderStatus.COMPLETED;
        List<OrderDetail> details = orderDetails.findByOrderIdOrderByIdAsc(order.getId());
        boolean allReviewed = true;
        if (!details.isEmpty()) {
            List<Integer> detailIds = details.stream().map(OrderDetail::getId).toList();
            var reviewedIds = reviews.findByOrderDetailIdIn(detailIds).stream()
                    .filter(r -> userId.equals(r.getUserId()))
                    .map(r -> r.getOrderDetail().getId())
                    .collect(java.util.stream.Collectors.toSet());
            allReviewed = reviewedIds.size() >= details.size();
        }
        return new OrderCard(order, linesOf(order.getId()), deadline,
                deliveredOrCompleted && withinWindow && !allReviewed,
                deliveredOrCompleted && withinWindow,
                order.getStatus() == OrderStatus.DELIVERED && withinWindow,
                allReviewed);
    }

    private List<OrderLineItem> linesOf(Integer orderId) {
        return orderDetails.findByOrderIdOrderByIdAsc(orderId).stream()
                .map(detail -> {
                    var variant = detail.getVariant();
                    var product = variant.getProduct();
                    String label = variant.getSize() != null ? variant.getSize() : variant.getSku();
                    if (variant.getShadeName() != null && !variant.getShadeName().isBlank()) {
                        label = label + " - " + variant.getShadeName();
                    }
                    return new OrderLineItem(product.getName(), label,
                            ProductImage.imageFor(product),
                            detail.getQuantity(), detail.getUnitPrice(), detail.getTotalPrice());
                })
                .toList();
    }

    private Orders ownOrder(Integer id, CustomUserDetails details) {
        return orders.findByIdAndUserId(id, details.getUser().getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng của bạn"));
    }

    /** Huy don: chi khi don chua giao (PENDING / CONFIRMED), phai nhap ly do. */
    @PostMapping("/orders/{id}/cancel")
    public String cancelOrder(@PathVariable Integer id,
                              @RequestParam String reason,
                              @AuthenticationPrincipal CustomUserDetails details,
                              RedirectAttributes redirect) {
        Orders order = ownOrder(id, details);
        if (reason == null || reason.isBlank()) {
            redirect.addFlashAttribute("orderError", "Vui lòng nhập lý do hủy đơn hàng");
            return "redirect:/account/orders?tab=PENDING_PAYMENT";
        }
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            redirect.addFlashAttribute("orderError", "Đơn này đã qua giai đoạn hủy, vui lòng liên hệ shop");
            return "redirect:/account/orders";
        }
        try {
            adminOrders.changeStatus(id, "CANCELLED", reason.trim());
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("orderError", e.getMessage());
            return "redirect:/account/orders";
        }
        redirect.addFlashAttribute("orderOk", "Đã hủy đơn " + order.getOrderCode());
        return "redirect:/account/orders?tab=CANCELLED";
    }

    /** Da nhan hang: don DELIVERED -> COMPLETED, chi trong 3 ngay sau khi giao. */
    @PostMapping("/orders/{id}/confirm-received")
    public String confirmReceived(@PathVariable Integer id,
                                  @AuthenticationPrincipal CustomUserDetails details,
                                  RedirectAttributes redirect) {
        Orders order = ownOrder(id, details);
        if (order.getStatus() != OrderStatus.DELIVERED) {
            redirect.addFlashAttribute("orderError", "Đơn này chưa ở trạng thái đã giao");
            return "redirect:/account/orders";
        }
        if (!OrderAutoCompleteService.isWithinReviewWindow(order, LocalDateTime.now())) {
            redirect.addFlashAttribute("orderError", "Đã hết hạn 3 ngày xác nhận, đơn sẽ tự hoàn thành");
            return "redirect:/account/orders";
        }
        try {
            adminOrders.completeOrder(id);
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("orderError", e.getMessage());
            return "redirect:/account/orders";
        }
        redirect.addFlashAttribute("orderOk", "Cảm ơn bạn đã xác nhận! Đơn " + order.getOrderCode() + " đã hoàn thành.");
        return "redirect:/account/orders?tab=COMPLETED";
    }

    /** Form yeu cau tra hang / hoan tien: mo trang moi, khach chon ly do + hinh thuc hoan tien. */
    @GetMapping("/orders/{id}/return")
    @Transactional(readOnly = true)
    public String returnForm(@PathVariable Integer id,
                             @AuthenticationPrincipal CustomUserDetails details,
                             Model model, RedirectAttributes redirect) {
        Orders order = ownOrder(id, details);
        if (!canReturn(order)) {
            redirect.addFlashAttribute("orderError", "Đơn này đã hết hạn trả hàng (3 ngày sau khi giao)");
            return "redirect:/account/orders";
        }
        model.addAttribute("order", order);
        model.addAttribute("lines", linesOf(order.getId()));
        model.addAttribute("returnForm", new ReturnForm());
        model.addAttribute("returnReasons", RETURN_REASONS);
        model.addAttribute("refundMethods", REFUND_METHODS);
        model.addAttribute("reviewDeadline", OrderAutoCompleteService.reviewDeadline(order));
        model.addAttribute("accountMenu", "orders");
        return "account/return-form";
    }

    /** Gui yeu cau tra hang: don DELIVERED / COMPLETED trong 3 ngay -> RETURNED. */
    @PostMapping("/orders/{id}/return")
    public String returnOrder(@PathVariable Integer id,
                              @Valid @ModelAttribute("returnForm") ReturnForm form,
                              BindingResult result,
                              @AuthenticationPrincipal CustomUserDetails details,
                              Model model, RedirectAttributes redirect) {
        Orders order = ownOrder(id, details);
        if (!canReturn(order)) {
            redirect.addFlashAttribute("orderError", "Đơn này đã hết hạn trả hàng (3 ngày sau khi giao)");
            return "redirect:/account/orders";
        }
        if (result.hasErrors()) {
            model.addAttribute("order", order);
            model.addAttribute("lines", linesOf(order.getId()));
            model.addAttribute("returnReasons", RETURN_REASONS);
            model.addAttribute("refundMethods", REFUND_METHODS);
            model.addAttribute("reviewDeadline", OrderAutoCompleteService.reviewDeadline(order));
            model.addAttribute("accountMenu", "orders");
            return "account/return-form";
        }
        try {
            String reason = form.getReason().trim()
                    + (form.getNote() == null || form.getNote().isBlank()
                            ? "" : " — " + form.getNote().trim())
                    + " (Hoàn tiền: " + refundLabel(form.getRefundMethod()) + ")";
            adminOrders.changeStatus(id, "RETURNED", reason);
        } catch (DataIntegrityViolationException e) {
            // Thuong la CHECK chk_orders_status trong DB chua co RETURNED
            // (chua chay migration 007) -> bao ro thay vi trang trang 500.
            redirect.addFlashAttribute("orderError",
                    "Không lưu được yêu cầu trả hàng: cơ sở dữ liệu chưa cập nhật ràng buộc trạng thái "
                            + "(chưa chạy migration 007_notifications_review_media.sql). Hãy chạy file 007 rồi thử lại.");
            return "redirect:/account/orders";
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("orderError", e.getMessage());
            return "redirect:/account/orders";
        }
        redirect.addFlashAttribute("orderOk", "Đã gửi yêu cầu trả hàng cho đơn " + order.getOrderCode()
                + ". Shop sẽ liên hệ với bạn để nhận lại hàng và hoàn tiền.");
        return "redirect:/account/orders?tab=RETURNED";
    }

    private boolean canReturn(Orders order) {
        boolean deliveredOrCompleted = order.getStatus() == OrderStatus.DELIVERED
                || order.getStatus() == OrderStatus.COMPLETED;
        return deliveredOrCompleted
                && OrderAutoCompleteService.isWithinReviewWindow(order, LocalDateTime.now());
    }

    private String refundLabel(String method) {
        return REFUND_METHODS.stream()
                .filter(m -> m.value().equals(method))
                .map(RefundOption::label)
                .findFirst()
                .orElse("Hoàn tiền về phương thức đã thanh toán");
    }

    /** Trang danh gia don: moi san pham trong don danh gia 1 lan, +200 diem / san pham. */
    public record ReviewLine(Integer detailId, String productName, String variantLabel,
                             String imageUrl, Integer quantity, Review review,
                             List<String> imageUrls, List<String> videoUrls) {
    }

    @GetMapping("/orders/{id}/review")
    @Transactional(readOnly = true)
    public String reviewForm(@PathVariable Integer id,
                             @AuthenticationPrincipal CustomUserDetails details,
                             Model model, RedirectAttributes redirect) {
        Orders order = ownOrder(id, details);
        if (!canReview(order)) {
            redirect.addFlashAttribute("orderError", "Đơn này đã hết hạn đánh giá (3 ngày sau khi giao)");
            return "redirect:/account/orders";
        }
        User user = currentUser(details);
        List<OrderDetail> orderDetailsList = orderDetails.findByOrderIdOrderByIdAsc(order.getId());
        List<Integer> detailIds = orderDetailsList.stream().map(OrderDetail::getId).toList();
        Map<Integer, Review> reviewedByDetail = detailIds.isEmpty() ? Map.of()
                : reviews.findByOrderDetailIdIn(detailIds).stream()
                        .filter(r -> user.getId().equals(r.getUserId()))
                        .collect(java.util.stream.Collectors.toMap(
                                r -> r.getOrderDetail().getId(), r -> r, (a, b) -> a));
        List<Integer> reviewedIds = reviewedByDetail.values().stream().map(Review::getId).toList();
        Map<Integer, List<String>> imagesByReview = new java.util.HashMap<>();
        Map<Integer, List<String>> videosByReview = new java.util.HashMap<>();
        if (!reviewedIds.isEmpty()) {
            var mediaByReview = reviewMedia.findByReviewIdIn(reviewedIds).stream()
                    .collect(java.util.stream.Collectors.groupingBy(m -> m.getReview().getId()));
            for (var e : mediaByReview.entrySet()) {
                imagesByReview.put(e.getKey(), e.getValue().stream()
                        .filter(m -> "IMAGE".equals(m.getMediaType()))
                        .map(com.luminous.model.ReviewMedia::getMediaUrl).toList());
                videosByReview.put(e.getKey(), e.getValue().stream()
                        .filter(m -> "VIDEO".equals(m.getMediaType()))
                        .map(com.luminous.model.ReviewMedia::getMediaUrl).toList());
            }
        }
        List<ReviewLine> lines = orderDetailsList.stream().map(detail -> {
            var variant = detail.getVariant();
            var product = variant.getProduct();
            String label = variant.getSize() != null ? variant.getSize() : variant.getSku();
            if (variant.getShadeName() != null && !variant.getShadeName().isBlank()) {
                label = label + " - " + variant.getShadeName();
            }
            Review done = reviewedByDetail.get(detail.getId());
            return new ReviewLine(detail.getId(), product.getName(), label,
                    ProductImage.imageFor(product), detail.getQuantity(), done,
                    done == null ? List.of() : imagesByReview.getOrDefault(done.getId(), List.of()),
                    done == null ? List.of() : videosByReview.getOrDefault(done.getId(), List.of()));
        }).toList();
        model.addAttribute("user", user);
        model.addAttribute("order", order);
        model.addAttribute("reviewLines", lines);
        ReviewBatchForm batchForm = new ReviewBatchForm();
        lines.forEach(l -> {
            ReviewBatchForm.ReviewItemForm item = new ReviewBatchForm.ReviewItemForm();
            item.setDetailId(l.detailId());
            item.setShowName(true);
            batchForm.getItems().add(item);
        });
        model.addAttribute("reviewBatchForm", batchForm);
        model.addAttribute("reviewDeadline", OrderAutoCompleteService.reviewDeadline(order));
        model.addAttribute("accountMenu", "orders");
        return "account/review-form";
    }

    /**
     * Gui danh gia ca don 1 lan (kieu Shopee): chi gui nhung san pham da chon
     * sao; moi san pham 1 lan, +200 diem / san pham; kem toi da 5 anh + 1 video.
     */
    @PostMapping("/orders/{id}/review")
    public String submitReviews(@PathVariable Integer id,
                                @ModelAttribute("reviewBatchForm") ReviewBatchForm form,
                                @AuthenticationPrincipal CustomUserDetails details,
                                RedirectAttributes redirect) {
        Orders order = ownOrder(id, details);
        if (!canReview(order)) {
            redirect.addFlashAttribute("orderError", "Đơn này đã hết hạn đánh giá (3 ngày sau khi giao)");
            return "redirect:/account/orders";
        }
        int done = 0;
        String lastError = null;
        for (ReviewBatchForm.ReviewItemForm item : form.getItems()) {
            if (item.getRating() == null) {
                continue;
            }
            OrderDetail detail = orderDetails.findById(item.getDetailId()).orElse(null);
            if (detail == null || !id.equals(detail.getOrderId())) {
                continue;
            }
            try {
                reviewService.submitReviewWithMedia(details.getUser().getId(), item.getDetailId(),
                        item.getRating(), item.getComment(), !item.isShowName(),
                        item.getImages(), item.getVideo());
                done++;
            } catch (IllegalArgumentException e) {
                lastError = e.getMessage();
            }
        }
        if (done == 0) {
            redirect.addFlashAttribute("orderError",
                    lastError != null ? lastError : "Vui lòng chọn sao cho ít nhất 1 sản phẩm");
            return "redirect:/account/orders/" + id + "/review";
        }
        // Ve trang chi tiet don + hien hop "Danh gia thanh cong"; bam OK thi
        // ve trang Don mua (xem popup o account/order-detail.html).
        redirect.addFlashAttribute("reviewSuccess", true);
        redirect.addFlashAttribute("reviewSuccessCount", done);
        redirect.addFlashAttribute("reviewSuccessPoints", done * CustomerReviewService.REVIEW_REWARD_POINTS);
        return "redirect:/account/orders/" + id;
    }

    private boolean canReview(Orders order) {
        boolean deliveredOrCompleted = order.getStatus() == OrderStatus.DELIVERED
                || order.getStatus() == OrderStatus.COMPLETED;
        return deliveredOrCompleted
                && OrderAutoCompleteService.isWithinReviewWindow(order, LocalDateTime.now());
    }

    /** Mua lai: them moi san pham trong don vao gio (moi loai 1 cai), bien the het hang thi bo qua. */
    @PostMapping("/orders/{id}/buy-again")
    public String buyAgain(@PathVariable Integer id,
                           @AuthenticationPrincipal CustomUserDetails details) {
        User user = currentUser(details);
        Orders order = ownOrder(id, details);
        orderDetails.findByOrderIdOrderByIdAsc(order.getId()).stream()
                .map(detail -> detail.getVariant().getId())
                .distinct()
                .forEach(variantId -> cartService.addItem(user, variantId, 1));
        return "redirect:/cart";
    }

    // ---------- Thong bao / Kho Voucher / Diem Thuong ----------

    @GetMapping("/notifications")
    public String notifications(@AuthenticationPrincipal CustomUserDetails details, Model model) {
        User user = currentUser(details);
        notifications.ensureSeededFromOrders(user);
        model.addAttribute("user", user);
        model.addAttribute("notificationList", notifications.listForUser(user.getId()));
        model.addAttribute("unread", notifications.unreadCount(user.getId()));
        model.addAttribute("accountMenu", "notifications");
        return "account/notifications";
    }

    /** Danh dau tat ca la da doc. */
    @PostMapping("/notifications/read-all")
    public String readAllNotifications(@AuthenticationPrincipal CustomUserDetails details) {
        notifications.markAllRead(details.getUser().getId());
        return "redirect:/account/notifications";
    }

    /** Bam vao 1 thong bao: danh dau da doc roi mo link cua no. */
    @GetMapping("/notifications/{id}/open")
    public String openNotification(@PathVariable Integer id,
                                   @AuthenticationPrincipal CustomUserDetails details) {
        Notification n = notifications.listForUser(details.getUser().getId()).stream()
                .filter(x -> id.equals(x.getId())).findFirst().orElse(null);
        if (n == null) {
            return "redirect:/account/notifications";
        }
        notifications.markRead(details.getUser().getId(), id);
        return "redirect:" + (n.getLink() != null && n.getLink().startsWith("/") ? n.getLink() : "/account/notifications");
    }

    /** Kho Voucher: cac ma giam gia dang mo (khach tu nhap ma o trang thanh toan). */
    @GetMapping("/vouchers")
    public String vouchers(@AuthenticationPrincipal CustomUserDetails details, Model model) {
        User user = currentUser(details);
        LocalDateTime now = LocalDateTime.now();
        List<Voucher> all = vouchers.findByIsActiveTrueOrderByEndDateAsc();
        List<Voucher> available = new ArrayList<>();
        List<Voucher> expired = new ArrayList<>();
        for (Voucher v : all) {
            boolean outOfDate = (v.getStartDate() != null && v.getStartDate().isAfter(now))
                    || (v.getEndDate() != null && v.getEndDate().isBefore(now));
            boolean outOfStock = v.getUsageLimit() != null && v.getUsedCount() != null
                    && v.getUsedCount() >= v.getUsageLimit();
            (outOfDate || outOfStock ? expired : available).add(v);
        }
        model.addAttribute("user", user);
        model.addAttribute("availableVouchers", available);
        model.addAttribute("expiredVouchers", expired);
        model.addAttribute("accountMenu", "vouchers");
        return "account/vouchers";
    }

    /** Dung ngay 1 voucher: luu ma vao session roi sang gio hang de thanh toan. */
    @PostMapping("/vouchers/{id}/use")
    public String useVoucher(@PathVariable Integer id, HttpSession session,
                             RedirectAttributes redirect) {
        Voucher v = vouchers.findById(id).orElse(null);
        if (v == null || !Boolean.TRUE.equals(v.getIsActive())) {
            redirect.addFlashAttribute("voucherError", "Voucher này không còn hiệu lực");
            return "redirect:/account/vouchers";
        }
        session.setAttribute(CartService.SESSION_VOUCHER_CODE, v.getVoucherCode());
        redirect.addFlashAttribute("voucherOk", "Đã áp mã " + v.getVoucherCode() + " cho đơn hàng tiếp theo");
        return "redirect:/cart";
    }

    /** Diem Thuong: so du + lich su cong/tru diem + cach kiem diem. */
    @GetMapping("/points")
    public String points(@AuthenticationPrincipal CustomUserDetails details, Model model) {
        User user = currentUser(details);
        model.addAttribute("user", user);
        model.addAttribute("pointBalance", user.getPointBalance() == null ? 0 : user.getPointBalance());
        model.addAttribute("pointHistory",
                pointTransactions.findByUserIdOrderByCreatedAtDesc(user.getId()));
        model.addAttribute("accountMenu", "points");
        return "account/points";
    }
}
