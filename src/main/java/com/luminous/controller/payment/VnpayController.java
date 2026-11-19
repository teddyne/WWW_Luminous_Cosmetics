package com.luminous.controller.payment;

import com.luminous.model.Orders;
import com.luminous.model.Payment;
import com.luminous.model.User;
import com.luminous.model.enums.PaymentMethod;
import com.luminous.model.enums.PaymentStatus;
import com.luminous.security.CustomUserDetails;
import com.luminous.service.payment.VnpayService;
import com.luminous.service.shop.CheckoutService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Nhan ket qua tu VNPay sandbox 2.1.0: Return URL (trinh duyet quay ve) +
 * IPN (VNPay goi ngam server-to-server).
 *
 * Chi xac nhan "da thanh toan" khi: chu ky dung + don ton tai + so tien khop
 * + don chua xac nhan truoc do (confirmVnpayPayment tra ALREADY_CONFIRMED
 * neu IPN/Return goi lap lai). IPN moi la noi cap nhat DB chinh thuc;
 * Return URL cung goi chung ham xac nhan de demo tren localhost van chay
 * (IPN can URL cong khai, vd ngrok, cau hinh tren trang quan tri merchant).
 * Trang thai don (PENDING) khong doi: tien PAID, don van cho shop xac nhan.
 */
@Controller
public class VnpayController {

    private final VnpayService vnpayService;
    private final CheckoutService checkoutService;

    public VnpayController(VnpayService vnpayService, CheckoutService checkoutService) {
        this.vnpayService = vnpayService;
        this.checkoutService = checkoutService;
    }

    /** Lay User dang dang nhap (giong CheckoutController). */
    private User currentUser(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails details) {
            return details.getUser();
        }
        return null;
    }

    /**
     * Khach tra tien xong, VNPay chuyen trinh duyet ve day (GET). Xac minh
     * xong thi dua ve trang success cua don (CheckoutController hien thong
     * bao theo tham so vnpay). Trang success tu kiem tra quyen so huu don.
     */
    @GetMapping("/payment/vnpay/return")
    public String vnpayReturn(@RequestParam Map<String, String> params) {
        String orderCode = params.get("vnp_TxnRef");
        if (orderCode == null || orderCode.isBlank()) {
            return "redirect:/cart";
        }
        String responseCode = params.get("vnp_ResponseCode");
        String txnStatus = params.get("vnp_TransactionStatus");
        if (!vnpayService.verifySignature(params)) {
            return "redirect:/checkout/success?orderCode=" + orderCode + "&vnpay=invalid";
        }
        if ("00".equals(responseCode) && "00".equals(txnStatus)) {
            BigDecimal amount = parseVnpAmount(params.get("vnp_Amount"));
            CheckoutService.VnpayConfirm result = checkoutService.confirmVnpayPayment(
                    orderCode, amount, params.get("vnp_TransactionNo"));
            if (result == CheckoutService.VnpayConfirm.NOT_FOUND
                    || result == CheckoutService.VnpayConfirm.INVALID_AMOUNT) {
                return "redirect:/checkout/success?orderCode=" + orderCode + "&vnpay=invalid";
            }
            return "redirect:/checkout/success?orderCode=" + orderCode + "&vnpay=success";
        }
        return "redirect:/checkout/success?orderCode=" + orderCode + "&vnpay=failed";
    }

    /**
     * VNPay goi ngam (server-to-server) de bao ket qua. Tra JSON theo dung
     * quy dinh cua VNPay: 00 = da nhan/xu ly xong; 01 = khong tim thay don;
     * 02 = don da xac nhan roi; 04 = sai so tien; 97 = sai chu ky; 99 = loi khac.
     * VNPay se goi lai IPN neu nhan ve ma khac 00.
     */
    @GetMapping("/payment/vnpay/ipn")
    @ResponseBody
    public Map<String, String> vnpayIpn(@RequestParam Map<String, String> params) {
        if (!vnpayService.verifySignature(params)) {
            return Map.of("RspCode", "97", "Message", "Invalid checksum");
        }
        String orderCode = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String txnStatus = params.get("vnp_TransactionStatus");
        if (!"00".equals(responseCode) || !"00".equals(txnStatus)) {
            // Giao dich that bai/huy: khong cap nhat don, nhung IPN da nhan xong.
            return Map.of("RspCode", "00", "Message", "Confirm Success");
        }
        BigDecimal amount = parseVnpAmount(params.get("vnp_Amount"));
        CheckoutService.VnpayConfirm result = checkoutService.confirmVnpayPayment(
                orderCode, amount, params.get("vnp_TransactionNo"));
        return switch (result) {
            case SUCCESS, ALREADY_CONFIRMED -> Map.of("RspCode", "00", "Message", "Confirm Success");
            case NOT_FOUND -> Map.of("RspCode", "01", "Message", "Order not found");
            case INVALID_AMOUNT -> Map.of("RspCode", "04", "Message", "Invalid amount");
        };
    }

    /**
     * Thanh toan lai 1 don VNPay chua tra (khach dong tab giua chung, het
     * han 15 phut, hoac tra that bai). Chi chu don duoc bam, don da PAID
     * thi dua ve trang success.
     */
    @GetMapping("/payment/vnpay/retry")
    public String vnpayRetry(Authentication auth, HttpServletRequest request,
                             @RequestParam String orderCode) {
        User user = currentUser(auth);
        Orders order = checkoutService.findByCode(orderCode).orElse(null);
        if (order == null || user == null || !order.getUser().getId().equals(user.getId())) {
            return "redirect:/cart";
        }
        Payment payment = checkoutService.paymentOf(order.getId()).orElse(null);
        if (payment == null || payment.getPaymentMethod() != PaymentMethod.VNPAY) {
            return "redirect:/checkout/success?orderCode=" + orderCode;
        }
        if (payment.getPaymentStatus() == PaymentStatus.PAID) {
            return "redirect:/checkout/success?orderCode=" + orderCode + "&vnpay=success";
        }
        if (order.getTotalAmount() == null
                || order.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return "redirect:/checkout/success?orderCode=" + orderCode;
        }
        return "redirect:" + vnpayService.createPaymentUrl(order,
                VnpayService.clientIp(request), null);
    }

    /** vnp_Amount = so tien x 100 -> chia 100 de ra so tien that. */
    private BigDecimal parseVnpAmount(String vnpAmount) {
        if (vnpAmount == null || vnpAmount.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(vnpAmount.trim()).divide(BigDecimal.valueOf(100));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}
