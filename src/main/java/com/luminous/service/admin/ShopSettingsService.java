package com.luminous.service.admin;

import com.luminous.model.ShopSetting;
import com.luminous.repository.ShopSettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ShopSettingsService {

    private static final Logger log = LoggerFactory.getLogger(ShopSettingsService.class);

    public static final String DEFAULT_GREETING = "Xin chào! Mình là trợ lý AI của Luminous. "
            + "Mình có thể tư vấn sản phẩm, so sánh giá, thêm vào giỏ hàng, tra cứu đơn hàng "
            + "và giải đáp chính sách của shop. Bạn cần tìm gì hôm nay?";

    private static final Map<String, String> DEFAULTS = new LinkedHashMap<>();
    private static final long CACHE_TTL_MS = 60_000;

    static {
        DEFAULTS.put("shop.name", "LUMINOUS");
        DEFAULTS.put("shop.logo", "/images/logo.png");
        DEFAULTS.put("shop.address", "12 Nguyễn Văn Bảo, Gò Vấp, TP.HCM");
        DEFAULTS.put("shop.hotline", "0972606925");
        DEFAULTS.put("shop.email", "admin@luminous.vn");
        DEFAULTS.put("shipping.fee", "30000");
        DEFAULTS.put("shipping.providers", "Giao hàng nhanh\nGiao hàng tiết kiệm\nViettel Post");
        DEFAULTS.put("payment.cod", "true");
        DEFAULTS.put("payment.banking", "true");
        DEFAULTS.put("payment.bankName", "");
        DEFAULTS.put("payment.bankAccount", "");
        DEFAULTS.put("payment.bankHolder", "");
        DEFAULTS.put("ai.enabled", "true");
        DEFAULTS.put("ai.dailyLimit", "20");
        DEFAULTS.put("ai.greeting", DEFAULT_GREETING);
    }

    public record ShopSettingsView(
            String shopName, String logoUrl, String address, String hotline, String email,
            BigDecimal shippingFee, List<String> shippingProviders,
            boolean paymentCod, boolean paymentBanking,
            String bankName, String bankAccount, String bankHolder,
            boolean aiEnabled, int aiDailyLimit, String aiGreeting) {
    }

    private final ShopSettingRepository repository;

    private volatile Map<String, String> cache = Map.of();
    private volatile long loadedAt = 0;

    public ShopSettingsService(ShopSettingRepository repository) {
        this.repository = repository;
    }

    /** Tat ca cai dat (gia tri mac dinh + gia tri da luu trong DB de len). */
    public Map<String, String> all() {
        Map<String, String> merged = new LinkedHashMap<>(DEFAULTS);
        merged.putAll(loadFromDb());
        return merged;
    }

    public String get(String key) {
        return all().getOrDefault(key, "");
    }

    public boolean isAiEnabled() {
        return bool("ai.enabled", true);
    }

    public int aiDailyLimit() {
        return intValue("ai.dailyLimit", 20);
    }

    public String aiGreeting() {
        String greeting = get("ai.greeting");
        return greeting.isBlank() ? DEFAULT_GREETING : greeting;
    }

    public BigDecimal shippingFee() {
        try {
            BigDecimal fee = new BigDecimal(get("shipping.fee"));
            return fee.compareTo(BigDecimal.ZERO) < 0 ? new BigDecimal("30000") : fee;
        } catch (NumberFormatException e) {
            return new BigDecimal("30000");
        }
    }

    public List<String> shippingProviders() {
        return Arrays.stream(get("shipping.providers").split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();
    }

    public boolean paymentCodEnabled() {
        return bool("payment.cod", true);
    }

    public boolean paymentBankingEnabled() {
        return bool("payment.banking", true);
    }

    public ShopSettingsView view() {
        Map<String, String> values = all();
        return new ShopSettingsView(
                values.get("shop.name"), values.get("shop.logo"),
                values.get("shop.address"), values.get("shop.hotline"), values.get("shop.email"),
                shippingFee(), shippingProviders(),
                bool("payment.cod", true), bool("payment.banking", true),
                values.get("payment.bankName"), values.get("payment.bankAccount"), values.get("payment.bankHolder"),
                bool("ai.enabled", true), intValue("ai.dailyLimit", 20), aiGreeting());
    }

    /**
     * Luu 1 nhom cai dat. Gia tri khong hop le bi bo qua (giu gia tri cu) va
     * tra ve danh sach canh bao de hien cho admin. Key la khong biet thi bo qua.
     */
    @Transactional
    public List<String> saveAll(Map<String, String> values) {
        List<String> warnings = new ArrayList<>();
        Map<String, String> current = loadFromDb();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue() == null ? "" : entry.getValue().trim();
            if (!DEFAULTS.containsKey(key)) {
                continue;
            }
            String problem = validate(key, value);
            if (problem != null) {
                warnings.add(problem);
                continue;
            }
            if (value.equals(current.get(key))) {
                continue;
            }
            ShopSetting row = repository.findBySettingKey(key)
                    .orElseGet(() -> new ShopSetting(key, value));
            row.setSettingValue(value);
            repository.save(row);
        }
        cache = Map.of();
        loadedAt = 0;
        return warnings;
    }

    private String validate(String key, String value) {
        return switch (key) {
            case "shipping.fee" -> {
                try {
                    BigDecimal fee = new BigDecimal(value);
                    yield fee.compareTo(BigDecimal.ZERO) >= 0 && fee.compareTo(new BigDecimal("10000000")) <= 0
                            ? null : "Phí vận chuyển phải từ 0 đến 10.000.000đ";
                } catch (NumberFormatException e) {
                    yield "Phí vận chuyển phải là số";
                }
            }
            case "ai.dailyLimit" -> {
                try {
                    int limit = Integer.parseInt(value);
                    yield limit >= 0 && limit <= 500
                            ? null : "Giới hạn lượt chat mỗi ngày phải từ 0 đến 500 (0 = không giới hạn)";
                } catch (NumberFormatException e) {
                    yield "Giới hạn lượt chat phải là số nguyên";
                }
            }
            case "payment.cod", "payment.banking", "ai.enabled" -> Set.of("true", "false").contains(value)
                    ? null : "Giá trị bật/tắt chỉ nhận 'true' hoặc 'false'";
            case "shop.name" -> value.isBlank() ? "Tên shop không được để trống" : null;
            case "ai.greeting" -> value.isBlank() ? "Lời chào AI không được để trống" : null;
            default -> null;
        };
    }

    private boolean bool(String key, boolean fallback) {
        return Boolean.parseBoolean(all().getOrDefault(key, String.valueOf(fallback)));
    }

    private int intValue(String key, int fallback) {
        try {
            return Integer.parseInt(all().getOrDefault(key, String.valueOf(fallback)));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /**
     * Doc tu DB co cache 60 giay. Neu bang ShopSetting chua co (chua chay
     * migration 011) thi tra ve rong va dung gia tri mac dinh, khong lam
     * hong trang: toan bo shop van chay binh thuong cho toi khi chay 011.
     */
    private Map<String, String> loadFromDb() {
        long now = System.currentTimeMillis();
        if (now - loadedAt < CACHE_TTL_MS) {
            return cache;
        }
        try {
            Map<String, String> values = new ConcurrentHashMap<>();
            for (ShopSetting row : repository.findAll()) {
                if (row.getSettingKey() != null) {
                    values.put(row.getSettingKey(), row.getSettingValue() == null ? "" : row.getSettingValue());
                }
            }
            cache = values;
            loadedAt = now;
            return values;
        } catch (Exception e) {
            log.warn("Khong doc duoc bang ShopSetting (chua chay migration 011?), dung gia tri mac dinh: {}",
                    e.getMessage());
            loadedAt = now;
            return Map.of();
        }
    }
}
