package com.luminous.service.skinquiz;

import com.luminous.dto.GuestRoutineSession;
import com.luminous.dto.ProductCard;
import com.luminous.dto.RoutineStep;
import com.luminous.model.ProductVariant;
import com.luminous.model.SkinQuizResult;
import com.luminous.model.SkinRoutine;
import com.luminous.model.User;
import com.luminous.repository.UserRepository;
import com.luminous.repository.product.ProductVariantRepository;
import com.luminous.repository.skinquiz.SkinQuizResultRepository;
import com.luminous.repository.skinquiz.SkinRoutineRepository;
import com.luminous.service.product.ProductService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class SkinQuizService {
    private final ProductVariantRepository variants;
    private final SkinQuizResultRepository results;
    private final SkinRoutineRepository routines;
    private final UserRepository users;
    private final ProductService productService;
    private final ObjectMapper mapper = new ObjectMapper();

    public SkinQuizService(ProductVariantRepository variants,
                           SkinQuizResultRepository results,
                           SkinRoutineRepository routines,
                           UserRepository users,
                           ProductService productService) {
        this.variants = variants;
        this.results = results;
        this.routines = routines;
        this.users = users;
        this.productService = productService;
    }

    // ---------- Du lieu tinh: 5 loai da (theo mo ta nguoi dung dua) ----------

    public record SkinInfo(String code, String name, String traits, String care) {
    }

    public static final List<SkinInfo> SKIN_INFOS = List.of(
            new SkinInfo("NORMAL", "Da thường",
                    "Cân bằng tốt giữa lượng dầu và nước, lỗ chân lông nhỏ, bề mặt mềm mịn, ít khuyết điểm, độ đàn hồi tốt và ít gặp rủi ro lão hóa sớm.",
                    "Duy trì làm sạch nhẹ nhàng, dưỡng ẩm cơ bản và dùng kem chống nắng hằng ngày."),
            new SkinInfo("DRY", "Da khô",
                    "Tuyến bã nhờn tiết ít dầu hơn mức bình thường, thiếu hụt độ ẩm và lipid, cảm giác căng cứng, sần sùi, dễ bong tróc vảy và dễ xuất hiện nếp nhăn sớm.",
                    "Ưu tiên cấp ẩm sâu, dùng kem dưỡng ẩm giàu dưỡng chất, tránh tắm nước quá nóng và sử dụng kem chống nắng."),
            new SkinInfo("OILY", "Da dầu / da nhờn",
                    "Tuyến bã nhờn hoạt động quá mức, bề mặt da bóng dầu, lỗ chân lông to và dễ bị bít tắc dẫn đến mụn trứng cá.",
                    "Rửa mặt sạch 2 lần/ngày, sử dụng sữa rửa mặt dịu nhẹ, kem dưỡng dạng gel mỏng nhẹ không chứa dầu (oil-free) và các hoạt chất hỗ trợ như salicylic acid."),
            new SkinInfo("COMBINATION", "Da hỗn hợp",
                    "Là sự kết hợp giữa da khô và da dầu; vùng chữ T (trán, mũi, cằm) thường tiết nhiều dầu và bóng nhờn, trong khi vùng má lại khô hoặc bình thường.",
                    "Chăm sóc riêng biệt theo từng vùng (kiềm dầu vùng chữ T và cấp ẩm cho vùng má)."),
            new SkinInfo("SENSITIVE", "Da nhạy cảm",
                    "Lớp màng bảo vệ da rất mỏng, dễ bị kích ứng, mẩn đỏ, ngứa rát, châm chích hoặc nổi ban khi tiếp xúc với hóa chất, mỹ phẩm lạ hay thời tiết thay đổi.",
                    "Dùng sản phẩm tối giản, không hương liệu, không cồn, bảng thành phần lành tính và luôn thử trước trên một vùng da.")
    );

    public record GoalInfo(String code, String label, List<String> keywords) {
    }

    public static final List<GoalInfo> GOALS = List.of(
            new GoalInfo("ACNE", "Giảm mụn", List.of("niacinamide", "zinc", "tràm trà", "bí đao", "mụn")),
            new GoalInfo("BRIGHT", "Sáng da, đều màu", List.of("vitamin c", "ascorbic", "niacinamide", "sáng da", "thâm")),
            new GoalInfo("HYDRATE", "Cấp ẩm sâu", List.of("hyaluronic", "ceramide", "glycerin", "panthenol", "ẩm")),
            new GoalInfo("ANTIAGE", "Chống lão hóa sớm", List.of("collagen", "vitamin c", "ceramide")),
            new GoalInfo("OIL", "Kiềm dầu, thu nhỏ lỗ chân lông", List.of("niacinamide", "zinc", "bí đao", "bóng dầu", "lỗ chân lông")),
            new GoalInfo("SOOTHE", "Làm dịu, giảm đỏ rát", List.of("rau má", "panthenol", "glycerin", "dịu nhẹ", "nhạy cảm"))
    );

    public record BudgetInfo(String code, String label, String note) {
    }

    public static final List<BudgetInfo> BUDGETS = List.of(
            new BudgetInfo("TIET_KIEM", "Tiết kiệm", "Ưu tiên variant rẻ nhất còn hàng ở từng bước."),
            new BudgetInfo("CAN_BANG", "Cân bằng", "Ưu tiên sản phẩm đang giảm giá, rồi mới tới giá thấp."),
            new BudgetInfo("CAO_CAP", "Cao cấp", "Ưu tiên dung tích lớn / giá cao nhất còn hàng ở từng bước."),
            new BudgetInfo("KHONG_GIOI_HAN", "Không giới hạn", "Chọn theo độ phù hợp và giảm giá, không ép theo giá.")
    );

    public SkinInfo skinInfo(String code) {
        return SKIN_INFOS.stream().filter(s -> s.code().equals(code)).findFirst()
                .orElse(SKIN_INFOS.get(0));
    }

    public String skinName(String code) {
        return skinInfo(code).name();
    }

    public String goalLabels(List<String> codes) {
        return codes.stream()
                .map(c -> GOALS.stream().filter(g -> g.code().equals(c)).findFirst()
                        .map(GoalInfo::label).orElse(c))
                .collect(Collectors.joining(", "));
    }

    public String budgetLabel(String code) {
        return BUDGETS.stream().filter(b -> b.code().equals(code)).findFirst()
                .map(BudgetInfo::label).orElse("");
    }

    public boolean validBudget(String code) {
        return code != null && BUDGETS.stream().anyMatch(b -> b.code().equals(code));
    }

    public boolean validGoal(String code) {
        return code != null && GOALS.stream().anyMatch(g -> g.code().equals(code));
    }

    // ---------- 1. Xac dinh loai da tu 5 cau hoi ----------

    /** Dem dap an; hoa thi theo thu tu SENSITIVE > COMBINATION > OILY > DRY > NORMAL. */
    public String detectSkinType(List<String> answers) {
        Map<String, Long> count = answers.stream()
                .filter(a -> a != null && !a.isBlank())
                .collect(Collectors.groupingBy(a -> a, Collectors.counting()));
        List<String> tieBreak = List.of("SENSITIVE", "COMBINATION", "OILY", "DRY", "NORMAL");
        return tieBreak.stream()
                .max(Comparator.comparingLong(c -> count.getOrDefault(c, 0L)))
                .orElse("NORMAL");
    }

    // ---------- 2. Chon variant cho 1 buoc ----------

    public record RoutineItem(String time, Integer categoryId, Integer variantId) {
    }

    /** Lich co dinh theo danh muc that trong DB: sang 3 buoc, toi 4 buoc. */
    public List<RoutineItem> buildRoutine(String skinType, List<String> goals, String budget) {
        List<RoutineItem> items = new ArrayList<>();
        // Buoi sang: rua mat -> serum -> chong nang.
        items.add(pick("MORNING", 2, skinType, goals, budget));
        items.add(pick("MORNING", 3, skinType, goals, budget));
        items.add(pick("MORNING", 4, skinType, goals, budget));
        // Buoi toi: tay trang -> rua mat -> serum -> duong am.
        items.add(pick("EVENING", 1, skinType, goals, budget));
        items.add(pick("EVENING", 2, skinType, goals, budget));
        items.add(pick("EVENING", 3, skinType, goals, budget));
        items.add(pick("EVENING", 5, skinType, goals, budget));
        return items.stream().filter(i -> i.variantId() != null).toList();
    }

    private RoutineItem pick(String time, Integer categoryId,
                             String skinType, List<String> goals, String budget) {
        List<ProductVariant> candidates =
                variants.findByProductCategoryIdAndIsActiveTrueAndStockQuantityGreaterThan(categoryId, 0);
        if (candidates.isEmpty()) {
            return new RoutineItem(time, categoryId, null);
        }
        candidates.sort((a, b) -> compareVariants(a, b, skinType, goals, budget));
        return new RoutineItem(time, categoryId, candidates.get(0).getId());
    }

    private int compareVariants(ProductVariant a, ProductVariant b,
                                String skinType, List<String> goals, String budget) {
        boolean aExact = isExactFit(a, skinType);
        boolean bExact = isExactFit(b, skinType);
        if (aExact != bExact) {
            return aExact ? -1 : 1;
        }
        int aScore = goalScore(a, goals);
        int bScore = goalScore(b, goals);
        if (aScore != bScore) {
            return Integer.compare(bScore, aScore);
        }
        if ("CAO_CAP".equals(budget)) {
            return b.displayPrice().compareTo(a.displayPrice());
        }
        if ("CAN_BANG".equals(budget) || "KHONG_GIOI_HAN".equals(budget)) {
            if (a.onSale() != b.onSale()) {
                return a.onSale() ? -1 : 1;
            }
        }
        return a.displayPrice().compareTo(b.displayPrice());
    }

    public boolean isExactFit(ProductVariant v, String skinType) {
        return suitableCodes(v.getSuitableSkinType()).contains(skinType);
    }

    public List<String> suitableCodes(String suitableSkinType) {
        if (suitableSkinType == null || suitableSkinType.isBlank()) {
            return List.of();
        }
        return Arrays.stream(suitableSkinType.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    public String suitableText(String suitableSkinType) {
        return suitableCodes(suitableSkinType).stream()
                .map(this::skinName).collect(Collectors.joining(", "));
    }

    private int goalScore(ProductVariant v, List<String> goals) {
        String haystack = ((v.getIngredients() != null ? v.getIngredients() : "") + " "
                + (v.getProduct() != null && v.getProduct().getDescription() != null
                        ? v.getProduct().getDescription() : "")).toLowerCase();
        int score = 0;
        for (String goal : goals) {
            boolean hit = GOALS.stream().filter(g -> g.code().equals(goal)).findFirst()
                    .map(g -> g.keywords().stream().anyMatch(haystack::contains))
                    .orElse(false);
            if (hit) {
                score++;
            }
        }
        return score;
    }

    public String usageFor(String time, Integer categoryId) {
        return switch (categoryId) {
            case 1 -> "Buổi tối: thấm ra bông tẩy trang, lau nhẹ toàn mặt, kể cả ngày không trang điểm để lấy đi bụi bẩn và kem chống nắng.";
            case 2 -> "MORNING".equals(time)
                    ? "Buổi sáng: làm ướt mặt, tạo bọt, massage nhẹ 30–60 giây rồi rửa sạch."
                    : "Buổi tối: rửa lại sau bước tẩy trang để làm sạch sâu mà không làm khô da.";
            case 3 -> "Sau khi rửa mặt, khi da còn hơi ẩm: thoa 2–3 giọt serum, vỗ nhẹ cho thấm rồi mới tới bước sau.";
            case 4 -> "Bước cuối buổi sáng: thoa đủ lượng kem chống nắng, thoa lại sau 2–3 giờ nếu hoạt động ngoài nắng nhiều.";
            case 5 -> "Bước khóa ẩm cuối buổi tối: thoa lượng vừa đủ, tập trung vùng khô, tránh bôi quá dày nếu da dầu.";
            default -> "Dùng theo hướng dẫn trên bao bì sản phẩm.";
        };
    }

    public String adviceFor(String skinType, List<String> goals, String budget) {
        SkinInfo skin = skinInfo(skinType);
        String budgetNote = BUDGETS.stream().filter(b -> b.code().equals(budget)).findFirst()
                .map(BudgetInfo::note).orElse("");
        return skin.name() + ": " + skin.care()
                + " Mục tiêu của bạn: " + goalLabels(goals) + ". "
                + "Ngân sách " + budgetLabel(budget).toLowerCase() + " — " + budgetNote;
    }

    // ---------- 3. Luu ket qua + lich ----------

    /** Dung chu trinh tu dap an (chua luu DB): dung chung cho khach (luu session)
     *  va cho tai khoan da dang nhap (luu DB o saveQuizAndRoutine). */
    public SkinRoutine buildTransientRoutine(int quizId, List<String> answers,
                                            List<String> goals, String budget) {
        String skinType = detectSkinType(answers);
        List<RoutineItem> items = buildRoutine(skinType, goals, budget);
        SkinRoutine routine = new SkinRoutine();
        routine.setQuizId(quizId);
        routine.setSkinType(skinType);
        routine.setGoals(String.join(",", goals));
        routine.setBudget(budget);
        try {
            routine.setRoutineJson(mapper.writeValueAsString(items));
        } catch (Exception e) {
            routine.setRoutineJson("[]");
        }
        routine.setCreatedAt(LocalDateTime.now());
        routine.setUpdatedAt(LocalDateTime.now());
        return routine;
    }

    /** Dung lai chu trinh tam tu session cua khach (khong cham DB). */
    public SkinRoutine transientRoutineFromSession(GuestRoutineSession guest) {
        SkinRoutine routine = new SkinRoutine();
        routine.setQuizId(guest.quizId());
        routine.setSkinType(guest.skinType());
        routine.setGoals(guest.goals());
        routine.setBudget(guest.budget());
        routine.setRoutineJson(guest.routineJson());
        routine.setCreatedAt(LocalDateTime.now());
        routine.setUpdatedAt(LocalDateTime.now());
        return routine;
    }

    /** Khach lam xong trac nghiem roi moi dang nhap: chuyen ket qua tam trong
     *  session xuong DB (giong nhu lam trac nghiem khi da dang nhap). */
    public SkinRoutine migrateGuestRoutine(User user, GuestRoutineSession guest) {
        List<String> goals = guest.goals() != null && !guest.goals().isBlank()
                ? Arrays.asList(guest.goals().split(",")) : List.of();
        List<RoutineJsonItem> items = parseItems(guest.routineJson());

        SkinQuizResult result = new SkinQuizResult();
        result.setUser(user);
        result.setQuizId(guest.quizId());
        result.setSkinType(guest.skinType());
        result.setSkincareAdvice(adviceFor(guest.skinType(), goals, guest.budget()));
        String distinctVariants = items.stream().map(RoutineJsonItem::variantId)
                .distinct().map(String::valueOf).collect(Collectors.joining(",", "[", "]"));
        result.setSuitableProducts(distinctVariants);
        result.setCompletedAt(LocalDateTime.now());
        results.save(result);

        SkinRoutine routine = routines.findByUserId(user.getId()).orElseGet(SkinRoutine::new);
        boolean isNew = routine.getId() == null;
        routine.setUser(user);
        routine.setQuizId(guest.quizId());
        routine.setSkinType(guest.skinType());
        routine.setGoals(guest.goals());
        routine.setBudget(guest.budget());
        routine.setRoutineJson(guest.routineJson());
        routine.setUpdatedAt(LocalDateTime.now());
        if (isNew) {
            routine.setCreatedAt(LocalDateTime.now());
        }
        routines.save(routine);

        user.setSkinQuizAsked(true);
        users.save(user);
        return routine;
    }

    /** Giai routineJson thanh danh sach buoc (loi thi tra ve danh sach rong). */
    private List<RoutineJsonItem> parseItems(String routineJson) {
        try {
            return mapper.readValue(routineJson, new TypeReference<List<RoutineJsonItem>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    public SkinRoutine saveQuizAndRoutine(User user, int quizId,
                                          List<String> answers, List<String> goals, String budget) {
        SkinRoutine tmp = buildTransientRoutine(quizId, answers, goals, budget);
        String skinType = tmp.getSkinType();
        List<RoutineJsonItem> items = parseItems(tmp.getRoutineJson());

        SkinQuizResult result = new SkinQuizResult();
        result.setUser(user);
        result.setQuizId(quizId);
        result.setSkinType(skinType);
        result.setSkincareAdvice(adviceFor(skinType, goals, budget));
        String distinctVariants = items.stream().map(RoutineJsonItem::variantId)
                .distinct().map(String::valueOf).collect(Collectors.joining(",", "[", "]"));
        result.setSuitableProducts(distinctVariants);
        result.setCompletedAt(LocalDateTime.now());
        results.save(result);

        SkinRoutine routine = routines.findByUserId(user.getId()).orElseGet(SkinRoutine::new);
        boolean isNew = routine.getId() == null;
        routine.setUser(user);
        routine.setQuizId(quizId);
        routine.setSkinType(skinType);
        routine.setGoals(String.join(",", goals));
        routine.setBudget(budget);
        routine.setRoutineJson(tmp.getRoutineJson());
        routine.setUpdatedAt(LocalDateTime.now());
        if (isNew) {
            routine.setCreatedAt(LocalDateTime.now());
        }
        routines.save(routine);

        user.setSkinQuizAsked(true);
        users.save(user);
        return routine;
    }

    // ---------- 4. Doc lai lich de hien thi ----------

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RoutineJsonItem(String time, Integer categoryId, Integer variantId) {
    }

    public List<RoutineStep> loadSteps(SkinRoutine routine) {
        List<RoutineJsonItem> items;
        try {
            items = mapper.readValue(routine.getRoutineJson(),
                    new TypeReference<List<RoutineJsonItem>>() {
                    });
        } catch (Exception e) {
            return List.of();
        }
        List<String> goals = routine.getGoals() != null && !routine.getGoals().isBlank()
                ? Arrays.asList(routine.getGoals().split(",")) : List.of();
        List<RoutineStep> steps = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            RoutineJsonItem item = items.get(i);
            Optional<ProductVariant> variant = productService.findVariant(item.variantId());
            if (variant.isEmpty() || !Boolean.TRUE.equals(variant.get().getIsActive())) {
                continue; // variant ngung ban: bo buoc, controller se bao so buoc thieu
            }
            ProductVariant v = variant.get();
            String categoryName = v.getProduct() != null && v.getProduct().getCategory() != null
                    ? v.getProduct().getCategory().getName() : "";
            // Chi doi sang san pham PHU HOP dung loai da cua nguoi dung, toi da 5.
            // Khac ban cu (lay 5 cai diem cao nhat ke ca loai "Tam phu hop"):
            // neu trong danh muc khong co variant nao dung loai da thi khong
            // hien cai khong phu hop de lap day, template se hien dong
            // "Hien chua co san pham tuong tu..." thay vi hien sai loai da.
            List<ProductCard> alternatives = productService.variantCardsForCategory(item.categoryId())
                    .stream()
                    .filter(c -> !c.getVariant().getId().equals(v.getId()))
                    .filter(c -> isExactFit(c.getVariant(), routine.getSkinType()))
                    .sorted((a, b) -> compareVariants(a.getVariant(), b.getVariant(),
                            routine.getSkinType(), goals, routine.getBudget()))
                    .limit(5)
                    .toList();
            steps.add(new RoutineStep(
                    i,
                    item.time(),
                    "MORNING".equals(item.time()) ? "Buổi sáng" : "Buổi tối",
                    item.categoryId(),
                    categoryName,
                    productService.cardForVariant(v),
                    isExactFit(v, routine.getSkinType()),
                    suitableText(v.getSuitableSkinType()),
                    usageFor(item.time(), item.categoryId()),
                    alternatives));
        }
        return steps;
    }

    /**
     * Tong tien mua 1 lan: tinh theo variant RIENG BIET, khong cong lap.
     * Sua rua mat va serum xuat hien ca sang lan toi nhung chi mua 1 chai,
     * nen tong = tien 5 san pham rieng, khong phai tong 7 buoc.
     */
    public BigDecimal totalPrice(List<RoutineStep> steps) {
        return steps.stream()
                .collect(Collectors.toMap(
                        s -> s.getCard().getVariant().getId(),
                        s -> s.getCard().displayPrice(),
                        (a, b) -> a))
                .values().stream()
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Doi variant o 1 buoc, chi chap nhan variant cung danh muc, dang ban, con hang. */
    public boolean swapStep(SkinRoutine routine, int stepIndex, Integer newVariantId) {
        List<RoutineJsonItem> items;
        try {
            items = mapper.readValue(routine.getRoutineJson(),
                    new TypeReference<List<RoutineJsonItem>>() {
                    });
        } catch (Exception e) {
            return false;
        }
        if (stepIndex < 0 || stepIndex >= items.size()) {
            return false;
        }
        Optional<ProductVariant> variant = productService.findVariant(newVariantId);
        if (variant.isEmpty()) {
            return false;
        }
        ProductVariant v = variant.get();
        RoutineJsonItem old = items.get(stepIndex);
        if (!old.categoryId().equals(v.getProduct().getCategory().getId())
                || !Boolean.TRUE.equals(v.getIsActive())
                || v.getStockQuantity() == null || v.getStockQuantity() <= 0) {
            return false;
        }
        items.set(stepIndex, new RoutineJsonItem(old.time(), old.categoryId(), newVariantId));
        try {
            routine.setRoutineJson(mapper.writeValueAsString(items));
        } catch (Exception e) {
            return false;
        }
        routine.setUpdatedAt(LocalDateTime.now());
        routines.save(routine);
        // Dong bo lai SkinQuizResult moi nhat cho khop lich vua doi.
        results.findFirstByUserIdOrderByCompletedAtDesc(routine.getUser().getId())
                .ifPresent(r -> {
                    String distinct = items.stream().map(RoutineJsonItem::variantId)
                            .distinct().map(String::valueOf)
                            .collect(Collectors.joining(",", "[", "]"));
                    r.setSuitableProducts(distinct);
                    results.save(r);
                });
        return true;
    }
}
