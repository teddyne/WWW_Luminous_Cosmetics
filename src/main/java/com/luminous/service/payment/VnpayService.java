package com.luminous.service.payment;

import com.luminous.model.Orders;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

/**
 * VNPay gateway 2.1.0 (sandbox): tao URL thanh toan + kiem tra chu ky tra ve.
 * Theo tai lieu chinh thuc https://sandbox.vnpayment.vn/apis/ :
 * - URL sandbox: https://sandbox.vnpayment.vn/paymentv2/vpcpay.html (GET)
 * - Chu ky HMAC-SHA512 tren chuoi key=value (da URL-encode) sap xep theo key;
 *   vnp_SecureHashType KHONG gui o 2.1.0; vnp_Amount = so tien x 100.
 * - vnp_BankCode: VNPAYQR (quet QR), VNBANK (the ATM), INTCARD (the quoc te);
 *   khong gui thi de khach chon tren trang VNPay.
 * - IPN URL khong nam trong request ma cau hinh tren trang quan tri merchant.
 */
@Service
public class VnpayService {

    private static final DateTimeFormatter VNP_DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final String tmnCode;
    private final String hashSecret;
    private final String payUrl;
    private final String returnUrl;

    public VnpayService(@Value("${vnpay.tmn-code:}") String tmnCode,
                        @Value("${vnpay.hash-secret:}") String hashSecret,
                        @Value("${vnpay.url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}") String payUrl,
                        @Value("${vnpay.return-url:http://localhost:8080/payment/vnpay/return}") String returnUrl) {
        this.tmnCode = tmnCode == null ? "" : tmnCode.trim();
        this.hashSecret = hashSecret == null ? "" : hashSecret.trim();
        this.payUrl = payUrl;
        this.returnUrl = returnUrl;
    }

    /** Chua nhap khoa sandbox thi khong cho tao URL (bao loi ro rang o checkout). */
    public boolean isConfigured() {
        return !tmnCode.isBlank() && !hashSecret.isBlank();
    }

    /**
     * IP nguoi tra (vnp_IpAddr). Sau proxy/tunnel (ngrok) thi lay IP that
     * tu X-Forwarded-For, khong thi lay IP truc tiep.
     */
    public static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Tao URL thanh toan cho 1 don. bankCode null thi de khach chon tren VNPay.
     * Ma QR het han sau 15 phut (vnp_ExpireDate).
     */
    public String createPaymentUrl(Orders order, String clientIp, String bankCode) {
        if (!isConfigured()) {
            throw new IllegalStateException("Chưa cấu hình VNPay sandbox (vnpay.tmn-code / vnpay.hash-secret).");
        }
        LocalDateTime now = LocalDateTime.now(VN_ZONE);
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", tmnCode);
        params.put("vnp_Amount", order.getTotalAmount().multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP).toPlainString());
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", order.getOrderCode());
        params.put("vnp_OrderInfo", "Thanh toan don hang " + order.getOrderCode());
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", returnUrl);
        params.put("vnp_IpAddr", clientIp == null || clientIp.isBlank() ? "127.0.0.1" : clientIp);
        params.put("vnp_CreateDate", now.format(VNP_DATE));
        params.put("vnp_ExpireDate", now.plusMinutes(15).format(VNP_DATE));
        if (bankCode != null && !bankCode.isBlank()) {
            params.put("vnp_BankCode", bankCode.trim());
        }
        String query = buildEncodedQuery(params);
        String secureHash = hmacSha512(hashSecret, query);
        return payUrl + "?" + query + "&vnp_SecureHash=" + secureHash;
    }

    /** Kiem tra chu ky VNPay tra ve (Return URL + IPN dung chung). */
    public boolean verifySignature(Map<String, String> params) {
        String received = params.get("vnp_SecureHash");
        if (received == null || received.isBlank() || !isConfigured()) {
            return false;
        }
        Map<String, String> data = new TreeMap<>(params);
        data.remove("vnp_SecureHash");
        data.remove("vnp_SecureHashType");
        String expected = hmacSha512(hashSecret, buildEncodedQuery(data));
        return received.equalsIgnoreCase(expected);
    }

    /** Chuoi key=value (URL-encode ca key lan value) noi bang &, key sap xep A-Z. */
    private String buildEncodedQuery(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (e.getValue() == null) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(encode(e.getKey())).append('=').append(encode(e.getValue()));
        }
        return sb.toString();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String hmacSha512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Không tạo được chữ ký HMAC-SHA512.", e);
        }
    }
}
