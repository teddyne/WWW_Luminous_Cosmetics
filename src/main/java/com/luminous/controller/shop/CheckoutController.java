package com.luminous.controller.shop;

import com.luminous.dto.CartLine;
import com.luminous.model.Address;
import com.luminous.model.Orders;
import com.luminous.model.Payment;
import com.luminous.model.User;
import com.luminous.model.Voucher;
import com.luminous.model.enums.PaymentMethod;
import com.luminous.model.enums.PaymentStatus;
import com.luminous.repository.address.AddressRepository;
import com.luminous.security.CustomUserDetails;
import com.luminous.service.shop.CartService;
import com.luminous.service.shop.CheckoutService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Dat hang (chi tien mat COD trong dot nay): chon dia chi + gio giao +
 * voucher + diem, roi tao don xong luon. Trang checkout chi co 1 form
 * duy nhat, nut bam nao cung gui ve POST /checkout/submit kem theo
 * action: place / apply-voucher / remove-voucher / add-address
 * (tranh form long form khong hop le trong HTML).
 */
@Controller
public class CheckoutController {

    /** Cac khung gio giao khach duoc chon (luu vao dau Orders.note). */
    private static final List<String> DELIVERY_SLOTS = List.of(
            "Giờ hành chính (08:00–17:00)",
            "Buổi sáng (08:00–12:00)",
            "Buổi chiều (13:00–17:00)",
            "Buổi tối (18:00–20:00)");

    private final CheckoutService checkoutService;
    private final CartService cartService;
    private final AddressRepository addresses;

    public CheckoutController(CheckoutService checkoutService,
                              CartService cartService,
                              AddressRepository addresses) {
        this.checkoutService = checkoutService;
        this.cartService = cartService;
        this.addresses = addresses;
    }

    /** Lay User dang dang nhap (giong CartController). */
    private User currentUser(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails details) {
            return details.getUser();
        }
        return null;
    }

    @GetMapping("/checkout")
    public String view(Authentication auth, HttpSession session,
                       @RequestParam(required = false) List<Integer> selectedVariantIds,
                       @RequestParam(required = false) Integer addressId,
                       Model model) {
        User user = currentUser(auth);
        if (user == null) {
            return "redirect:/login";
        }
        List<CartLine> lines = checkoutService.selectedLines(user, selectedVariantIds);
        if (lines.isEmpty()) {
            return "redirect:/cart";
        }

        // Ma giam gia mang tu gio sang, phai hop le voi chinh cac dong duoc chon.
        String code = (String) session.getAttribute(CartService.SESSION_VOUCHER_CODE);
        Voucher applied = null;
        String voucherError = null;
        if (code != null && !code.isBlank()) {
            applied = cartService.findVoucherByCode(code).orElse(null);
            if (applied == null) {
                session.removeAttribute(CartService.SESSION_VOUCHER_CODE);
            } else {
                voucherError = cartService.validateVoucher(applied, lines, LocalDateTime.now());
                if (voucherError != null) {
                    applied = null;
                }
            }
        }

        CartService.Totals totals = cartService.totals(lines, applied);
        BigDecimal afterDiscount = totals.total().add(CheckoutService.SHIPPING_FEE);
        int balance = user.getPointBalance() == null ? 0 : user.getPointBalance();
        int pointsMax = Math.min(balance, afterDiscount.intValue());

        List<Address> myAddresses = addresses.findByUserIdOrderByIsDefaultDescIdAsc(user.getId());
        Integer selectedAddressId = addressId;
        if (selectedAddressId == null && !myAddresses.isEmpty()) {
            selectedAddressId = myAddresses.get(0).getId();
        }

        model.addAttribute("lines", lines);
        model.addAttribute("selectedVariantIds", selectedVariantIds);
        model.addAttribute("addresses", myAddresses);
        model.addAttribute("selectedAddressId", selectedAddressId);
        model.addAttribute("deliverySlots", DELIVERY_SLOTS);
        model.addAttribute("appliedVoucher", applied);
        model.addAttribute("voucherError", voucherError);
        model.addAttribute("subtotalText", CartLine.formatVnd(totals.subtotal()));
        model.addAttribute("discountText", CartLine.formatVnd(totals.discount()));
        model.addAttribute("shippingText", CartLine.formatVnd(CheckoutService.SHIPPING_FEE));
        model.addAttribute("totalText", CartLine.formatVnd(afterDiscount));
        model.addAttribute("totalValue", afterDiscount);
        model.addAttribute("pointBalance", balance);
        model.addAttribute("pointsMax", pointsMax);
        return "checkout/checkout";
    }

    @PostMapping("/checkout/submit")
    public String submit(Authentication auth, HttpSession session,
                         @RequestParam(defaultValue = "place") String action,
                         @RequestParam(required = false) List<Integer> selectedVariantIds,
                         @RequestParam(required = false) Integer addressId,
                         @RequestParam(required = false) String deliverySlot,
                         @RequestParam(required = false) String note,
                         @RequestParam(defaultValue = "0") int usePoints,
                         @RequestParam(required = false) String paymentMethod,
                         @RequestParam(required = false) String voucherCode,
                         @RequestParam(required = false) String recipientName,
                         @RequestParam(required = false) String phone,
                         @RequestParam(required = false) String province,
                         @RequestParam(required = false) String ward,
                         @RequestParam(required = false) String detailAddress,
                         @RequestParam(defaultValue = "false") boolean isDefault,
                         RedirectAttributes redirect) {
        User user = currentUser(auth);
        if (user == null) {
            return "redirect:/login";
        }
        String back = backToCheckout(selectedVariantIds, null);

        switch (action) {
            case "apply-voucher" -> {
                List<CartLine> lines = checkoutService.selectedLines(user, selectedVariantIds);
                if (voucherCode == null || voucherCode.isBlank()) {
                    redirect.addFlashAttribute("voucherApplyError", "Bạn chưa nhập mã giảm giá.");
                } else {
                    Voucher v = cartService.findVoucherByCode(voucherCode.trim()).orElse(null);
                    if (v == null) {
                        redirect.addFlashAttribute("voucherApplyError", "Mã giảm giá không tồn tại.");
                    } else {
                        String reason = cartService.validateVoucher(v, lines, LocalDateTime.now());
                        if (reason != null) {
                            redirect.addFlashAttribute("voucherApplyError", reason);
                        } else {
                            session.setAttribute(CartService.SESSION_VOUCHER_CODE, v.getVoucherCode());
                        }
                    }
                    redirect.addFlashAttribute("voucherCode", voucherCode.trim());
                }
                return "redirect:/checkout" + back;
            }
            case "remove-voucher" -> {
                session.removeAttribute(CartService.SESSION_VOUCHER_CODE);
                return "redirect:/checkout" + back;
            }
            case "add-address" -> {
                if (isBlank(recipientName) || isBlank(phone) || isBlank(province)
                        || isBlank(ward) || isBlank(detailAddress)) {
                    redirect.addFlashAttribute("checkoutError", "Bạn cần nhập đủ họ tên, số điện thoại và địa chỉ.");
                    return "redirect:/checkout" + back;
                }
                List<Address> mine = addresses.findByUserIdOrderByIsDefaultDescIdAsc(user.getId());
                if (mine.isEmpty()) {
                    isDefault = true;
                }
                if (isDefault) {
                    for (Address old : mine) {
                        if (Boolean.TRUE.equals(old.getIsDefault())) {
                            old.setIsDefault(false);
                            addresses.save(old);
                        }
                    }
                }
                Address a = new Address();
                a.setUser(user);
                a.setRecipientName(recipientName.trim());
                a.setPhone(phone.trim());
                a.setProvince(province.trim());
                // Tu 01/07/2025 khong con cap huyen: luu chuoi rong (cot district
                // trong DB la NOT NULL nen khong de null duoc); fullAddress()
                // bo qua phan rong nen dia chi hien ra van sach.
                a.setDistrict("");
                a.setWard(ward.trim());
                a.setDetailAddress(detailAddress.trim());
                a.setIsDefault(isDefault);
                a = addresses.save(a);
                return "redirect:/checkout" + backToCheckout(selectedVariantIds, a.getId());
            }
            case "edit-address" -> {
                Address target = addressId == null ? null
                        : addresses.findByIdAndUserId(addressId, user.getId()).orElse(null);
                if (target == null) {
                    redirect.addFlashAttribute("checkoutError", "Không tìm thấy địa chỉ cần sửa.");
                    return "redirect:/checkout" + back;
                }
                if (isBlank(recipientName) || isBlank(phone) || isBlank(province)
                        || isBlank(ward) || isBlank(detailAddress)) {
                    redirect.addFlashAttribute("checkoutError", "Bạn cần nhập đủ họ tên, số điện thoại và địa chỉ.");
                    return "redirect:/checkout" + back;
                }
                List<Address> mine = addresses.findByUserIdOrderByIsDefaultDescIdAsc(user.getId());
                if (mine.size() == 1) {
                    isDefault = true;
                }
                if (isDefault) {
                    for (Address old : mine) {
                        if (!old.getId().equals(target.getId()) && Boolean.TRUE.equals(old.getIsDefault())) {
                            old.setIsDefault(false);
                            addresses.save(old);
                        }
                    }
                }
                target.setRecipientName(recipientName.trim());
                target.setPhone(phone.trim());
                target.setProvince(province.trim());
                target.setDistrict("");
                target.setWard(ward.trim());
                target.setDetailAddress(detailAddress.trim());
                target.setIsDefault(isDefault);
                addresses.save(target);
                return "redirect:/checkout" + backToCheckout(selectedVariantIds, target.getId());
            }
            case "delete-address" -> {
                Address target = addressId == null ? null
                        : addresses.findByIdAndUserId(addressId, user.getId()).orElse(null);
                if (target != null) {
                    boolean wasDefault = Boolean.TRUE.equals(target.getIsDefault());
                    addresses.delete(target);
                    if (wasDefault) {
                        List<Address> remaining = addresses.findByUserIdOrderByIsDefaultDescIdAsc(user.getId());
                        if (!remaining.isEmpty() && !Boolean.TRUE.equals(remaining.get(0).getIsDefault())) {
                            remaining.get(0).setIsDefault(true);
                            addresses.save(remaining.get(0));
                        }
                    }
                }
                return "redirect:/checkout" + back;
            }
            default -> {
                // Dat hang (action=place).
            }
        }

        PaymentMethod method = PaymentMethod.from(paymentMethod);
        if (method != PaymentMethod.COD) {
            redirect.addFlashAttribute("checkoutError",
                    "Đợt này mới có thanh toán tiền mặt (COD).");
            return "redirect:/checkout" + back;
        }

        String code = (String) session.getAttribute(CartService.SESSION_VOUCHER_CODE);
        CheckoutService.PlacedOrder placed;
        try {
            placed = checkoutService.placeOrder(user, selectedVariantIds, addressId,
                    deliverySlot, note, usePoints, method, code);
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("checkoutError", e.getMessage());
            return "redirect:/checkout" + back;
        }
        session.removeAttribute(CartService.SESSION_VOUCHER_CODE);
        return "redirect:/checkout/success?orderCode=" + placed.order().getOrderCode();
    }

    /** Trang xac nhan sau khi dat hang. */
    @GetMapping("/checkout/success")
    public String success(Authentication auth, @RequestParam String orderCode, Model model) {
        User user = currentUser(auth);
        Orders order = checkoutService.findByCode(orderCode).orElse(null);
        if (order == null || user == null || !order.getUser().getId().equals(user.getId())) {
            return "redirect:/cart";
        }
        fillSuccessModel(order, null, model);
        return "checkout/success";
    }

    /** Model chung cho trang xac nhan. */
    public void fillSuccessModel(Orders order, String message, Model model) {
        Payment payment = checkoutService.paymentOf(order.getId()).orElse(null);
        model.addAttribute("order", order);
        model.addAttribute("details", checkoutService.detailsOf(order.getId()));
        model.addAttribute("payment", payment);
        model.addAttribute("paid", payment != null && payment.getPaymentStatus() == PaymentStatus.PAID);
        model.addAttribute("message", message);
        model.addAttribute("orderCode", order.getOrderCode());
    }

    private String backToCheckout(List<Integer> selectedVariantIds, Integer addressId) {
        StringBuilder sb = new StringBuilder();
        if (selectedVariantIds != null && !selectedVariantIds.isEmpty()) {
            sb.append('?');
            for (int i = 0; i < selectedVariantIds.size(); i++) {
                if (i > 0) {
                    sb.append('&');
                }
                sb.append("selectedVariantIds=").append(selectedVariantIds.get(i));
            }
            if (addressId != null) {
                sb.append("&addressId=").append(addressId);
            }
        } else if (addressId != null) {
            sb.append("?addressId=").append(addressId);
        }
        return sb.toString();
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
