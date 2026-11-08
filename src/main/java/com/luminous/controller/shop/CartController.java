package com.luminous.controller.shop;

import com.luminous.dto.CartLine;
import com.luminous.model.User;
import com.luminous.model.Voucher;
import com.luminous.security.CustomUserDetails;
import com.luminous.service.shop.CartService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;
import java.util.stream.Collectors;

/**
 * Gio hang (DB Cart/CartItem, chi cho CUSTOMER/ADMIN da dang nhap).
 * Trang gio mac dinh chon het dong con hang; bo chon chi doi tong tien
 * tren trinh duyet (JS), nut "Tien hanh dat hang" gui cac dong duoc chon
 * sang /checkout (buoc tiep theo).
 */
@Controller
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    /** Lay User dang dang nhap (giong SkinQuizController). */
    private User currentUser(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails details) {
            return details.getUser();
        }
        return null;
    }

    @GetMapping("/cart")
    public String view(Authentication auth, HttpSession session, Model model) {
        User user = currentUser(auth);
        List<CartLine> lines = cartService.lines(user);

        // Ma giam gia dang ap dung (luu o session) phai hop le voi gio hien tai
        // moi duoc tru tien; het han/do don khong du thi bo qua va bao ly do.
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
        model.addAttribute("lines", lines);
        model.addAttribute("totalLines", lines.size());
        model.addAttribute("selectedCount", lines.stream().filter(CartLine::available).count());
        model.addAttribute("subtotalText", CartLine.formatVnd(totals.subtotal()));
        model.addAttribute("hasDiscount", totals.discount().compareTo(BigDecimal.ZERO) > 0);
        model.addAttribute("discountText", CartLine.formatVnd(totals.discount()));
        model.addAttribute("totalText", CartLine.formatVnd(totals.total()));
        model.addAttribute("appliedVoucher", applied);
        model.addAttribute("voucherError", voucherError);
        if (applied != null) {
            model.addAttribute("voucherType", applied.getDiscountType());
            model.addAttribute("voucherValue", applied.getDiscountValue());
            model.addAttribute("voucherMax", applied.getMaxDiscountAmount());
            model.addAttribute("voucherMin", applied.getMinOrderValue());
            model.addAttribute("voucherScope", applied.getScopeType());
            model.addAttribute("voucherScopeCategories", cartService.scopeCategoryIds(applied).stream()
                    .map(String::valueOf).collect(Collectors.joining(",")));
            model.addAttribute("voucherScopeProducts", cartService.scopeProductIds(applied).stream()
                    .map(String::valueOf).collect(Collectors.joining(",")));
        }
        return "cart/cart";
    }

    @PostMapping("/cart/add")
    public String add(Authentication auth,
                      @RequestParam Integer variantId,
                      @RequestParam(defaultValue = "1") int quantity,
                      @RequestParam(defaultValue = "false") boolean buyNow) {
        cartService.addItem(currentUser(auth), variantId, quantity);
        // buyNow se dan sang /checkout khi buoc thanh toan xong; tam thoi ve lai gio hang.
        return "redirect:/cart";
    }

    @PostMapping("/cart/update")
    public String update(Authentication auth,
                         @RequestParam Integer variantId,
                         @RequestParam int quantity) {
        cartService.updateQuantity(currentUser(auth), variantId, quantity);
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove")
    public String remove(Authentication auth, @RequestParam Integer variantId) {
        cartService.removeItem(currentUser(auth), variantId);
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove-selected")
    public String removeSelected(Authentication auth,
                                 @RequestParam(required = false) List<Integer> selectedVariantIds) {
        cartService.removeItems(currentUser(auth), selectedVariantIds);
        return "redirect:/cart";
    }

    @PostMapping("/cart/apply-voucher")
    public String applyVoucher(Authentication auth, HttpSession session,
                               @RequestParam String code,
                               RedirectAttributes redirect) {
        List<CartLine> lines = cartService.lines(currentUser(auth));
        String clean = code == null ? "" : code.trim();
        Voucher voucher = cartService.findVoucherByCode(clean).orElse(null);
        String error = voucher == null
                ? "Mã giảm giá không tồn tại."
                : cartService.validateVoucher(voucher, lines, LocalDateTime.now());
        if (error != null) {
            redirect.addFlashAttribute("voucherApplyError", error);
            redirect.addFlashAttribute("voucherCode", clean);
        } else {
            session.setAttribute(CartService.SESSION_VOUCHER_CODE, voucher.getVoucherCode());
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove-voucher")
    public String removeVoucher(HttpSession session) {
        session.removeAttribute(CartService.SESSION_VOUCHER_CODE);
        return "redirect:/cart";
    }
}
