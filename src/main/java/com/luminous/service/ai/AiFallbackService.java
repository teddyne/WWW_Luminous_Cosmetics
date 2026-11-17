package com.luminous.service.ai;

import com.luminous.dto.CartLine;
import com.luminous.dto.ProductCard;
import com.luminous.dto.ReviewView;
import com.luminous.dto.ai.AiProductView;
import com.luminous.model.User;
import com.luminous.service.ai.AiOrderDraftService.DraftItem;
import com.luminous.service.ai.AiOrderDraftService.DraftView;
import com.luminous.service.ai.AiPolicyDocuments.PolicyDoc;
import com.luminous.service.product.ProductService;
import com.luminous.service.shop.CartService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AiFallbackService {

    public record FallbackAnswer(String text, List<AiProductView> products, boolean fallback, DraftView draft) {
        public FallbackAnswer(String text, List<AiProductView> products, boolean fallback) {
            this(text, products, fallback, null);
        }
    }

    private static final Set<String> ORDER_WORDS = Set.of(
            "don hang", "tra don", "ma don", "trang thai don", "don cua toi", "kiem tra don");
    private static final Set<String> REVIEW_WORDS = Set.of(
            "danh gia", "review", "nhan xet", "co tot khong", "co nen mua");
    private static final Set<String> POLICY_WORDS = Set.of(
            "ship", "van chuyen", "phi ship", "giao hang", "thanh toan", "doi tra",
            "hoan tien", "tra hang", "voucher", "diem thuong", "tai khoan",
            "mua tai cua hang", "cua hang", "cod", "chuyen khoan", "uu dai",
            "khuyen mai", "bao hanh", "phi van chuyen", "mien phi ship");
    private static final Set<String> STOP_WORDS = Set.of(
            "them", "vao", "gio", "hang", "mua", "giup", "hay", "cho", "toi", "minh",
            "san", "pham", "cai", "lay", "dat", "can", "tim", "ban", "co", "la",
            "mot", "cua", "em", "anh", "chi", "nhe", "nha", "duoc", "khong", "voi",
            "o", "a", "di", "nao", "gi", "nay", "kia", "ma", "va", "hoac", "loai",
            "xem", "tu", "van", "goi", "y", "so", "sanh", "gia", "bao", "nhieu",
            "nen", "dung", "chon", "chay", "nhat", "tot", "re", "moi", "hot",
            "ben", "pho", "bien", "noi", "bat", "yeu", "thich", "cao", "thap");
    private static final Set<String> GREETING_FIRST_WORDS = Set.of("hello", "hi", "hey", "alo");
    private static final Pattern LEADING_QUANTITY = Pattern.compile("^(\\d+)\\s+(.*)$");

    private final ProductService productService;
    private final AiPolicyDocuments policies;
    private final CartService cartService;
    private final AiOrderDraftService draftService;

    public FallbackAnswer answer(String message, User user, HttpSession session) {
        String normalized = AiTextUtils.normalize(message);
        if (containsAny(normalized, ORDER_WORDS)) {
            return new FallbackAnswer(
                    "Bạn đăng nhập rồi vào mục Đơn mua trong Hồ sơ để tra cứu đơn hàng: ở đó có trạng thái, ngày giao và chi tiết từng đơn.",
                    List.of(), true);
        }
        if (isSmallTalk(normalized)) {
            return new FallbackAnswer(smallTalkReply(normalized), List.of(), true);
        }
        if (rankingKind(normalized) != null && !isAddToCartIntent(normalized)) {
            FallbackAnswer ranked = handleRanking(normalized);
            if (ranked != null) {
                return ranked;
            }
        }
        if (isCompareIntent(normalized)) {
            FallbackAnswer compared = handleCompare(message, normalized);
            if (compared != null) {
                return compared;
            }
        }
        if (isDraftIntent(normalized)) {
            return handleDraft(message, normalized, user, session);
        }
        if (isAddToCartIntent(normalized)) {
            return handleAddToCart(message, user, session);
        }
        List<ScoredCard> ranked = searchProductsRanked(message);
        if (containsAny(normalized, REVIEW_WORDS) && !ranked.isEmpty()) {
            ProductCard first = ranked.get(0).card();
            return new FallbackAnswer(
                    "Dưới đây là thông tin tìm được trong shop:\n"
                            + productLine(first) + "\n" + reviewSummaryText(first.getProduct().getId(), first.name()),
                    List.of(AiProductView.from(first)), true);
        }
        if (containsAny(normalized, POLICY_WORDS)) {
            List<PolicyDoc> policyDocs = policies.search(message, 2);
            if (!policyDocs.isEmpty()) {
                StringBuilder text = new StringBuilder();
                for (PolicyDoc doc : policyDocs) {
                    text.append("\n").append(doc.content());
                }
                return new FallbackAnswer(text.toString(), List.of(), true);
            }
        }
        if (!ranked.isEmpty()) {
            return listProductsAnswer(ranked);
        }
        return new FallbackAnswer(
                "Mình chưa tìm thấy sản phẩm phù hợp với \""
                        + (message == null ? "" : message.trim())
                        + "\". Bạn thử gõ tên sản phẩm, thương hiệu hoặc loại sản phẩm cần tìm (ví dụ: sữa rửa mặt, serum, kem chống nắng), hoặc dùng ô tìm kiếm trên thanh menu của shop.",
                List.of(), true);
    }

    public static boolean isSmallTalk(String normalized) {
        if (normalized == null || normalized.isBlank()) {
            return false;
        }
        if (normalized.contains("may gio") || normalized.contains("bay gio")
                || normalized.contains("hom nay ngay") || normalized.contains("hom nay la")
                || normalized.contains("thu may") || normalized.contains("ngay may")) {
            return true;
        }
        if (normalized.contains("cam on") || normalized.contains("thank")) {
            return true;
        }
        if (normalized.contains("tam biet") || normalized.equals("bye") || normalized.startsWith("bye ")) {
            return true;
        }
        String first = normalized.split(" ")[0];
        if (GREETING_FIRST_WORDS.contains(first)) {
            return true;
        }
        return normalized.equals("chao") || normalized.startsWith("chao ")
                || normalized.equals("xin chao") || normalized.startsWith("xin chao ");
    }

    public static boolean isDraftIntent(String normalized) {
        if (normalized == null) {
            return false;
        }
        return normalized.contains("soan don") || normalized.contains("tao don nhap")
                || normalized.contains("tao don") || normalized.contains("len don");
    }

    private String smallTalkReply(String normalized) {
        if (normalized.contains("may gio") || normalized.contains("bay gio")
                || normalized.contains("hom nay ngay") || normalized.contains("hom nay la")
                || normalized.contains("thu may") || normalized.contains("ngay may")) {
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
            return "Bây giờ là " + now.format(DateTimeFormatter.ofPattern("HH:mm"))
                    + ", " + vietnameseWeekday(now.getDayOfWeek())
                    + " ngày " + now.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    + ". Bạn cần mình hỗ trợ gì thêm không?";
        }
        if (normalized.contains("cam on") || normalized.contains("thank")) {
            return "Không có gì đâu, cảm ơn bạn đã ghé Luminous! Bạn cần mình hỗ trợ thêm gì không?";
        }
        if (normalized.contains("tam biet") || normalized.equals("bye") || normalized.startsWith("bye ")) {
            return "Tạm biệt bạn, hẹn gặp lại ở Luminous nhé!";
        }
        return "Xin chào bạn! Mình là trợ lý AI của Luminous. Bạn cần tìm sản phẩm, so sánh giá, thêm vào giỏ hay tra cứu đơn hàng?";
    }

    private String vietnameseWeekday(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> "thứ Hai";
            case TUESDAY -> "thứ Ba";
            case WEDNESDAY -> "thứ Tư";
            case THURSDAY -> "thứ Năm";
            case FRIDAY -> "thứ Sáu";
            case SATURDAY -> "thứ Bảy";
            case SUNDAY -> "Chủ nhật";
        };
    }

    private enum RankingKind {
        BEST_SELLER, CHEAPEST, BEST_RATED, MOST_EXPENSIVE
    }

    private RankingKind rankingKind(String normalized) {
        if (normalized == null) {
            return null;
        }
        if (normalized.contains("ban chay") || normalized.contains("mua nhieu nhat")
                || normalized.contains("pho bien nhat") || normalized.contains("yeu thich nhat")) {
            return RankingKind.BEST_SELLER;
        }
        if (normalized.contains("re nhat") || normalized.contains("gia thap nhat")) {
            return RankingKind.CHEAPEST;
        }
        if (normalized.contains("danh gia cao nhat") || normalized.contains("tot nhat")) {
            return RankingKind.BEST_RATED;
        }
        if (normalized.contains("dat nhat") || normalized.contains("cao cap nhat")) {
            return RankingKind.MOST_EXPENSIVE;
        }
        return null;
    }

    private String removeRankingWords(String normalized) {
        String text = normalized;
        for (String phrase : List.of(
                "duoc danh gia cao nhat", "danh gia cao nhat", "ban chay nhat",
                "mua nhieu nhat", "pho bien nhat", "yeu thich nhat",
                "gia thap nhat", "cao cap nhat",
                "ban chay", "re nhat", "tot nhat", "dat nhat")) {
            text = text.replace(phrase, " ");
        }
        return text.replaceAll("\\s+", " ").trim();
    }

    private FallbackAnswer handleRanking(String normalized) {
        RankingKind kind = rankingKind(normalized);
        if (kind == null) {
            return null;
        }
        List<String> keywords = extractKeywords(removeRankingWords(normalized));
        List<ProductCard> candidates;
        if (keywords.isEmpty()) {
            candidates = new ArrayList<>(searchAll());
        } else {
            List<ScoredCard> ranked = searchProductsRanked(String.join(" ", keywords));
            candidates = ranked.stream()
                    .filter(s -> s.score() >= keywords.size())
                    .map(ScoredCard::card)
                    .toList();
        }
        if (candidates.isEmpty()) {
            return null;
        }
        List<ProductCard> sorted = new ArrayList<>(candidates);
        switch (kind) {
            case BEST_SELLER -> sorted.sort((a, b) -> {
                int c = Long.compare(b.getSoldCount(), a.getSoldCount());
                return c != 0 ? c : Double.compare(b.getRating(), a.getRating());
            });
            case CHEAPEST -> sorted.sort((a, b) -> {
                int c = comparePriceAsc(a.displayPrice(), b.displayPrice());
                return c != 0 ? c : Double.compare(b.getRating(), a.getRating());
            });
            case BEST_RATED -> sorted.sort((a, b) -> {
                int c = Double.compare(b.getRating(), a.getRating());
                return c != 0 ? c : Long.compare(b.getReviewCount(), a.getReviewCount());
            });
            case MOST_EXPENSIVE -> sorted.sort((a, b) -> {
                int c = comparePriceAsc(b.displayPrice(), a.displayPrice());
                return c != 0 ? c : Double.compare(b.getRating(), a.getRating());
            });
        }
        String criterion = switch (kind) {
            case BEST_SELLER -> "bán chạy nhất";
            case CHEAPEST -> "rẻ nhất";
            case BEST_RATED -> "được đánh giá cao nhất";
            case MOST_EXPENSIVE -> "cao cấp nhất";
        };
        StringBuilder text = new StringBuilder("Đây là các ").append(categoryLabel(sorted))
                .append(" ").append(criterion).append(" bên Luminous:\n");
        List<AiProductView> views = new ArrayList<>();
        for (ProductCard card : sorted.stream().limit(5).toList()) {
            views.add(AiProductView.from(card));
            text.append("\n- ").append(productLine(card));
            if (kind == RankingKind.BEST_SELLER) {
                text.append(" — đã bán ").append(card.getSoldCount());
            }
        }
        text.append("\n\nBạn cho mình biết loại da và khoảng ngân sách của bạn để mình gợi ý cụ thể hơn nhé!");
        return new FallbackAnswer(text.toString(), views, true);
    }

    private int comparePriceAsc(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
        return a.compareTo(b);
    }

    private String categoryLabel(List<ProductCard> cards) {
        for (ProductCard card : cards) {
            if (card.getProduct().getCategory() != null
                    && card.getProduct().getCategory().getName() != null
                    && !card.getProduct().getCategory().getName().isBlank()) {
                return card.getProduct().getCategory().getName().toLowerCase();
            }
        }
        return "sản phẩm";
    }

    private FallbackAnswer handleDraft(String message, String normalized, User user, HttpSession session) {
        String itemsPart = normalized;
        int gomIndex = normalized.indexOf("gom");
        if (gomIndex >= 0) {
            itemsPart = normalized.substring(gomIndex + 3).trim();
        } else {
            itemsPart = removeDraftWords(normalized);
        }
        List<String> parts = new ArrayList<>();
        for (String part : itemsPart.split(" va ")) {
            if (!part.isBlank()) {
                parts.add(part.trim());
            }
        }
        if (parts.isEmpty()) {
            return new FallbackAnswer(
                    "Bạn cho mình biết đơn nháp gồm những sản phẩm nào nhé (ví dụ: soạn đơn gồm 1 gel rửa mặt Cocoon và 1 son Dior).",
                    List.of(), true);
        }
        List<DraftItem> items = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        List<ProductCard> foundCards = new ArrayList<>();
        List<Integer> quantities = new ArrayList<>();
        for (String part : parts) {
            int qty = 1;
            String rest = part;
            Matcher matcher = LEADING_QUANTITY.matcher(part);
            if (matcher.matches()) {
                qty = Math.max(1, Integer.parseInt(matcher.group(1)));
                rest = matcher.group(2);
            }
            List<ScoredCard> ranked = searchProductsRanked(rest);
            ProductCard chosen = null;
            if (!ranked.isEmpty()) {
                chosen = firstInStockVariant(ranked.get(0).card());
            }
            if (chosen == null) {
                missing.add(rest.isBlank() ? part : rest);
                continue;
            }
            items.add(new DraftItem(chosen.getVariant().getId(), qty, chosen.displayPrice()));
            foundCards.add(ranked.get(0).card());
            quantities.add(qty);
        }
        if (!missing.isEmpty()) {
            StringBuilder text = new StringBuilder("Mình chưa tìm thấy món này trong shop nên chưa soạn được đơn nháp:\n");
            for (String name : missing) {
                text.append("\n- ").append(name);
            }
            text.append("\n\nBạn kiểm tra lại tên sản phẩm rồi nói lại giúp mình nhé (ví dụ: soạn đơn gồm 1 gel rửa mặt Cocoon và 1 son Dior).");
            return new FallbackAnswer(text.toString(), List.of(), true);
        }
        Integer userId = user != null ? user.getId() : null;
        String sessionId = session != null ? session.getId() : null;
        DraftView draft;
        try {
            draft = draftService.createDraft(userId, sessionId, items);
        } catch (Exception e) {
            return new FallbackAnswer(
                    "Chưa soạn được đơn nháp lúc này (" + e.getMessage() + "). Bạn thử lại sau nhé.",
                    List.of(), true);
        }
        StringBuilder text = new StringBuilder("Đã soạn đơn nháp #").append(draft.id()).append(":\n");
        List<AiProductView> views = new ArrayList<>();
        for (int i = 0; i < foundCards.size(); i++) {
            ProductCard card = foundCards.get(i);
            views.add(AiProductView.from(card));
            text.append("\n- **").append(card.name()).append("** x ").append(quantities.get(i))
                    .append(" — ").append(CartLine.formatVnd(
                            card.displayPrice().multiply(BigDecimal.valueOf(quantities.get(i)))));
        }
        text.append("\n\nTạm tính: ").append(CartLine.formatVnd(draft.subtotal()))
                .append(" • Phí vận chuyển: ").append(CartLine.formatVnd(draft.shippingFee()))
                .append(" • Tổng: ").append(CartLine.formatVnd(draft.total()))
                .append("\nĐơn nháp hết hạn sau 30 phút và chưa phải đơn hàng thật. Bạn bấm nút chuyển vào giỏ để xem lại và xác nhận đặt hàng.");
        return new FallbackAnswer(text.toString(), views, true, draft);
    }

    private String removeDraftWords(String normalized) {
        return normalized
                .replace("soan don nhap", " ").replace("soan don", " ")
                .replace("tao don nhap", " ").replace("tao don", " ")
                .replace("len don", " ").replace("gom", " ")
                .replaceAll("\\s+", " ").trim();
    }

    private ProductCard firstInStockVariant(ProductCard card) {
        for (ProductCard variantCard : productService.activeVariantCards(card.getProduct().getId())) {
            Integer stock = variantCard.getVariant().getStockQuantity();
            if (stock != null && stock > 0) {
                return variantCard;
            }
        }
        return null;
    }

    private FallbackAnswer handleAddToCart(String message, User user, HttpSession session) {
        List<ScoredCard> ranked = searchProductsRanked(message);
        if (ranked.isEmpty()) {
            return new FallbackAnswer(
                    "Mình chưa tìm thấy sản phẩm phù hợp với \""
                            + (message == null ? "" : message.trim())
                            + "\" nên chưa thêm được vào giỏ hàng. Bạn thử gõ tên sản phẩm đầy đủ hơn, hoặc dùng ô tìm kiếm trên thanh menu của shop.",
                    List.of(), true);
        }
        ScoredCard top = ranked.get(0);
        boolean strongUnique = ranked.size() == 1
                || (top.score() >= 2 && top.score() > ranked.get(1).score());
        if (!strongUnique) {
            StringBuilder text = new StringBuilder(
                    "Mình tìm thấy nhiều sản phẩm phù hợp, bạn cho mình biết muốn thêm sản phẩm nào nhé:\n");
            List<AiProductView> views = new ArrayList<>();
            for (ScoredCard scored : ranked.stream().limit(5).toList()) {
                views.add(AiProductView.from(scored.card()));
                text.append("\n- ").append(productLine(scored.card()));
            }
            return new FallbackAnswer(text.toString(), views, true);
        }
        ProductCard card = top.card();
        ProductCard chosen = firstInStockVariant(card);
        if (chosen == null) {
            return new FallbackAnswer(
                    "Sản phẩm **" + card.name() + "** hiện đã hết hàng nên chưa thêm được vào giỏ.",
                    List.of(AiProductView.from(card)), true);
        }
        if (user == null && session == null) {
            return new FallbackAnswer(
                    "Mình tìm thấy **" + card.name()
                            + "** (" + chosen.displayPriceText() + ") nhưng chưa thêm được vào giỏ lúc này, bạn thử lại sau nhé.",
                    List.of(AiProductView.from(card)), true);
        }
        int qty = parseQuantity(message);
        boolean added = user != null
                ? cartService.addItem(user, chosen.getVariant().getId(), qty)
                : cartService.addGuestItem(session, chosen.getVariant().getId(), qty);
        if (!added) {
            return new FallbackAnswer(
                    "Không thêm được **" + card.name() + "** vào giỏ hàng, sản phẩm có thể vừa hết hàng.",
                    List.of(AiProductView.from(card)), true);
        }
        return new FallbackAnswer(
                "Đã thêm " + qty + " x **" + card.name() + "** (" + chosen.sizeLabel() + ", "
                        + chosen.displayPriceText() + ") vào giỏ hàng. Bạn có thể xem lại giỏ ở trang Giỏ hàng.",
                List.of(AiProductView.from(card)), true);
    }

    private FallbackAnswer listProductsAnswer(List<ScoredCard> ranked) {
        StringBuilder text = new StringBuilder("Mình tìm thấy các sản phẩm phù hợp trong shop:\n");
        List<AiProductView> views = new ArrayList<>();
        for (ScoredCard scored : ranked.stream().limit(5).toList()) {
            views.add(AiProductView.from(scored.card()));
            text.append("\n- ").append(productLine(scored.card()));
        }
        return new FallbackAnswer(text.toString(), views, true);
    }

    private String productLine(ProductCard card) {
        StringBuilder line = new StringBuilder("**").append(card.name()).append("** (").append(card.brand()).append("): ")
                .append(card.displayPriceText());
        if (card.onSale()) {
            line.append(" (giá gốc ").append(card.priceText()).append(")");
        }
        if (card.getReviewCount() > 0) {
            line.append(" - ").append(String.format("%.1f", card.getRating()))
                    .append(" sao (").append(card.getReviewCount()).append(" đánh giá)");
        }
        return line.toString();
    }

    private record ScoredCard(ProductCard card, int score) {
    }

    private List<ScoredCard> searchProductsRanked(String message) {
        List<String> keywords = extractKeywords(message);
        if (keywords.isEmpty()) {
            return List.of();
        }
        List<ProductCard> found = searchPhrase(extractPhrase(message));
        List<ScoredCard> scored;
        if (!found.isEmpty()) {
            scored = score(found, keywords);
        } else {
            List<ProductCard> all = searchAll();
            if (!allKeywordsMatchCatalog(all, keywords)) {
                return List.of();
            }
            scored = score(all, keywords).stream()
                    .filter(s -> s.score() > 0)
                    .toList();
        }
        List<ScoredCard> ranked = new ArrayList<>(scored);
        ranked.sort((a, b) -> {
            int c = Integer.compare(b.score(), a.score());
            if (c != 0) {
                return c;
            }
            c = Double.compare(b.card().getRating(), a.card().getRating());
            if (c != 0) {
                return c;
            }
            return Long.compare(b.card().getSoldCount(), a.card().getSoldCount());
        });
        return ranked;
    }

    private boolean allKeywordsMatchCatalog(List<ProductCard> catalog, List<String> keywords) {
        for (String keyword : keywords) {
            boolean matched = false;
            for (ProductCard card : catalog) {
                if (haystackOf(card).contains(keyword)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return false;
            }
        }
        return true;
    }

    private String haystackOf(ProductCard card) {
        return AiTextUtils.normalize(card.name() + " " + card.brand()
                + (card.getProduct().getCategory() != null && card.getProduct().getCategory().getName() != null
                ? " " + card.getProduct().getCategory().getName() : ""));
    }

    private List<ScoredCard> score(List<ProductCard> cards, List<String> keywords) {
        List<ScoredCard> result = new ArrayList<>();
        for (ProductCard card : cards) {
            String haystack = haystackOf(card);
            int score = 0;
            for (String keyword : keywords) {
                if (haystack.contains(keyword)) {
                    score++;
                }
            }
            result.add(new ScoredCard(card, score));
        }
        return result;
    }

    private List<ProductCard> searchPhrase(String phrase) {
        if (phrase == null || phrase.isBlank()) {
            return List.of();
        }
        try {
            return productService.search(phrase, null, null, null, null, null, null, false);
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<ProductCard> searchAll() {
        try {
            return productService.search("", null, null, null, null, null, null, false);
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<String> extractKeywords(String message) {
        return AiTextUtils.tokens(message).stream()
                .filter(t -> t.length() > 1)
                .filter(t -> !STOP_WORDS.contains(t))
                .filter(t -> !t.matches(".*\\d.*"))
                .toList();
    }

    private String extractPhrase(String message) {
        if (message == null) {
            return "";
        }
        List<String> kept = new ArrayList<>();
        for (String token : message.split("[^\\p{L}0-9]+")) {
            if (token.isBlank()) {
                continue;
            }
            String normalized = AiTextUtils.normalize(token);
            if (normalized.isEmpty() || STOP_WORDS.contains(normalized) || normalized.matches(".*\\d.*")) {
                continue;
            }
            kept.add(token);
        }
        return String.join(" ", kept);
    }

    private int parseQuantity(String message) {
        for (String token : AiTextUtils.tokens(message)) {
            if (token.matches("[0-9]{1,2}")) {
                int qty = Integer.parseInt(token);
                if (qty >= 1) {
                    return qty;
                }
            }
        }
        return 1;
    }

    private boolean isAddToCartIntent(String normalized) {
        if (isCompareIntent(normalized)) {
            return false;
        }
        if (normalized.contains("them") && normalized.contains("gio")) {
            return true;
        }
        if (normalized.contains("vao gio")) {
            return true;
        }
        return normalized.contains("dat mua") || normalized.contains("mua ngay");
    }

    private boolean isCompareIntent(String normalized) {
        if (normalized.contains("so sanh") || normalized.contains("cai nao")) {
            return true;
        }
        if (normalized.contains(" hay ")) {
            return true;
        }
        if ((normalized.contains(" nen ") || normalized.startsWith("nen "))
                && (normalized.contains("mua") || normalized.contains("chon") || normalized.contains("dung"))
                && !normalized.contains("co nen mua")) {
            return true;
        }
        return false;
    }

    private FallbackAnswer handleCompare(String message, String normalized) {
        List<ProductCard> picked = new ArrayList<>();
        if (normalized.contains(" hay ")) {
            for (String side : normalized.split(" hay ")) {
                List<ScoredCard> ranked = searchProductsRanked(side);
                if (!ranked.isEmpty()) {
                    addIfNew(picked, ranked.get(0).card());
                }
            }
        }
        if (picked.size() < 2) {
            for (ScoredCard scored : searchProductsRanked(message)) {
                addIfNew(picked, scored.card());
                if (picked.size() >= 2) {
                    break;
                }
            }
        }
        if (picked.isEmpty()) {
            return null;
        }
        List<AiProductView> views = new ArrayList<>();
        StringBuilder text = new StringBuilder("Mình so sánh nhanh các sản phẩm bạn đang phân vân nhé:\n");
        for (ProductCard card : picked) {
            views.add(AiProductView.from(card));
            text.append("\n- ").append(productLine(card));
            String skin = card.getVariant() != null ? card.getVariant().getSuitableSkinType() : null;
            if (skin != null && !skin.isBlank()) {
                text.append(" - Phù hợp: ").append(skin);
            }
            if (card.getProduct().getCategory() != null && card.getProduct().getCategory().getName() != null) {
                text.append(" (").append(card.getProduct().getCategory().getName()).append(")");
            }
        }
        text.append("\n\nGợi ý chung: bạn chọn theo nhu cầu của mình (công dụng, loại da) và ngân sách. ");
        text.append("Bạn cho mình biết loại da và khoảng ngân sách của bạn để mình gợi ý cụ thể hơn nhé!");
        return new FallbackAnswer(text.toString(), views, true);
    }

    private void addIfNew(List<ProductCard> picked, ProductCard card) {
        for (ProductCard existing : picked) {
            if (existing.getProduct().getId().equals(card.getProduct().getId())) {
                return;
            }
        }
        picked.add(card);
    }

    public String reviewSummaryText(Integer productId, String productName) {
        try {
            ProductService.ReviewSummary summary = productService.reviewSummary(productId);
            if (summary == null || summary.total() == 0) {
                return "Sản phẩm " + productName + " chưa có đánh giá nào.";
            }
            StringBuilder text = new StringBuilder("Đánh giá cho **").append(productName).append("**: ")
                    .append(String.format("%.1f", summary.average())).append(" sao trên ")
                    .append(summary.total()).append(" đánh giá (");
            for (int star = 5; star >= 1; star--) {
                text.append(star).append(" sao: ").append(summary.countFor(star));
                if (star > 1) {
                    text.append(", ");
                }
            }
            text.append(").");
            List<ReviewView> comments = productService.reviewPage(productId, null, 0, 2).getContent();
            for (ReviewView view : comments) {
                if (view.comment() != null && !view.comment().isBlank()) {
                    text.append("\n- Khách đánh giá ").append(view.rating()).append(" sao: \"")
                            .append(view.comment()).append("\"");
                }
            }
            return text.toString();
        } catch (Exception e) {
            return "Chưa lấy được đánh giá cho " + productName + " lúc này.";
        }
    }

    private boolean containsAny(String normalized, Set<String> words) {
        for (String word : words) {
            if (normalized.contains(AiTextUtils.normalize(word))) {
                return true;
            }
        }
        return false;
    }
}
