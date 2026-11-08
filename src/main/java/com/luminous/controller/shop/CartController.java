package com.luminous.controller.shop;

import com.luminous.dto.CartLine;
import com.luminous.model.User;
import com.luminous.model.Voucher;
import com.luminous.security.CustomUserDetails;
import com.luminous.service.shop.CartService;
import jakarta.servlet.http.HttpServletRequest;
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

    /** Cac dong gio theo dung loai tai khoan (DB cho da dang nhap, session cho khach). */
    private List<CartLine> linesFor(User user, HttpSession session) {
        return user != null ? cartService.lines(user) : cartService.guestLines(session);
    }

    @GetMapping("/cart")
    public String view(Authentication auth, HttpSession session, Model model) {
        User user = currentUser(auth);
        // Vua dang nhap: chuyen gio tam luc con la khach vao gio DB.
        if (user != null) {
            cartService.mergeGuestCart(user, session);
        }
        List<CartLine> lines = linesFor(user, session);

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
        model.addAttribute("isGuest", user == null);
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
    public String add(Authentication auth, HttpSession session, HttpServletRequest request,
                      @RequestParam Integer variantId,
                      @RequestParam(defaultValue = "1") int quantity,
                      @RequestParam(defaultValue = "false") boolean buyNow,
                      RedirectAttributes redirect) {
        User user = currentUser(auth);
        boolean ok = user != null
                ? cartService.addItem(user, variantId, quantity)
                : cartService.addGuestItem(session, variantId, quantity);
        // Mua ngay thi ve gio hang nhu cu; them thuong thi o lai trang hien
        // tai, chi hien thong bao (toast o header) la da them thanh cong.
        if (buyNow) {
            return "redirect:/cart";
        }
        if (ok) {
            redirect.addFlashAttribute("cartAdded", "Đã thêm sản phẩm vào giỏ hàng thành công.");
        } else {
            redirect.addFlashAttribute("cartAddError", "Không thêm được sản phẩm (đã hết hàng hoặc ngừng bán).");
        }
        return "redirect:" + backUrl(request);
    }

    /** Trang vua dung de quay lai sau khi them vao gio; chi chap nhan URL
     *  cung ung dung (chong open redirect), mac dinh ve trang chu. */
    private String backUrl(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return "/";
        }
        try {
            java.net.URI uri = new java.net.URI(referer);
            if (uri.getHost() != null && !uri.getHost().equalsIgnoreCase(request.getServerName())) {
                return "/";
            }
            String path = uri.getRawPath();
            if (path == null || path.isBlank()) {
                return "/";
            }
            String query = uri.getRawQuery();
            return query == null || query.isBlank() ? path : path + "?" + query;
        } catch (Exception e) {
            return "/";
        }
    }

    @PostMapping("/cart/update")
    public String update(Authentication auth, HttpSession session,
                         @RequestParam Integer variantId,
                         @RequestParam int quantity) {
        User user = currentUser(auth);
        if (user != null) {
            cartService.updateQuantity(user, variantId, quantity);
        } else {
            cartService.updateGuestQuantity(session, variantId, quantity);
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove")
    public String remove(Authentication auth, HttpSession session, @RequestParam Integer variantId) {
        User user = currentUser(auth);
        if (user != null) {
            cartService.removeItem(user, variantId);
        } else {
            cartService.removeGuestItem(session, variantId);
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove-selected")
    public String removeSelected(Authentication auth, HttpSession session,
                                 @RequestParam(required = false) List<Integer> selectedVariantIds) {
        User user = currentUser(auth);
        if (user != null) {
            cartService.removeItems(user, selectedVariantIds);
        } else {
            cartService.removeGuestItems(session, selectedVariantIds);
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/apply-voucher")
    public String applyVoucher(Authentication auth, HttpSession session,
                               @RequestParam String code,
                               RedirectAttributes redirect) {
        List<CartLine> lines = linesFor(currentUser(auth), session);
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
