package com.luminous.controller.skinquiz;

import com.luminous.dto.GuestRoutineSession;
import com.luminous.dto.RoutineStep;
import com.luminous.model.SkinRoutine;
import com.luminous.model.User;
import com.luminous.repository.UserRepository;
import com.luminous.repository.skinquiz.SkinRoutineRepository;
import com.luminous.security.CustomUserDetails;
import com.luminous.service.shop.CartService;
import com.luminous.service.skinquiz.SkinQuizDataLoader;
import com.luminous.service.skinquiz.SkinQuizService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Controller
public class SkinQuizController {
    private final SkinQuizDataLoader quizLoader;
    private final SkinQuizService skinService;
    private final SkinRoutineRepository routines;
    private final UserRepository users;
    private final CartService cartService;

    public SkinQuizController(SkinQuizDataLoader quizLoader,
                              SkinQuizService skinService,
                              SkinRoutineRepository routines,
                              UserRepository users,
                              CartService cartService) {
        this.quizLoader = quizLoader;
        this.skinService = skinService;
        this.routines = routines;
        this.users = users;
        this.cartService = cartService;
    }

    /** Ket qua trac nghiem tam cua khach trong session (null neu chua lam). */
    private GuestRoutineSession guestSession(HttpServletRequest request) {
        if (request.getSession(false) == null) {
            return null;
        }
        Object value = request.getSession(false).getAttribute(GuestRoutineSession.SESSION_KEY);
        return value instanceof GuestRoutineSession guest ? guest : null;
    }

    private User currentUser(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails details) {
            return details.getUser();
        }
        return null;
    }

    /** Chu trinh hien tai de hien (dung chung cho building, routine, buy-all).
     *  Ket qua vua lam luc con la khach luon thang: dang nhap xong phai thay
     *  dung ket qua do, ke ca tai khoan da co chu trinh cu truoc do (ket qua
     *  tam trong session duoc chuyen xuong DB va xoa khoi session ngay).
     *  Tra ve null khi chua co ket qua nao (khach chua lam / tai khoan chua co). */
    private record ResolvedRoutine(SkinRoutine routine, boolean isGuest) {
    }

    private ResolvedRoutine resolveRoutine(Authentication auth, HttpServletRequest request) {
        User user = currentUser(auth);
        GuestRoutineSession guest = guestSession(request);
        if (guest != null) {
            if (user != null) {
                SkinRoutine routine = skinService.migrateGuestRoutine(user, guest);
                request.getSession().removeAttribute(GuestRoutineSession.SESSION_KEY);
                return new ResolvedRoutine(routine, false);
            }
            return new ResolvedRoutine(skinService.transientRoutineFromSession(guest), true);
        }
        if (user == null) {
            return null;
        }
        return routines.findByUserId(user.getId())
                .map(routine -> new ResolvedRoutine(routine, false))
                .orElse(null);
    }

    // ---------- Trang hoi lan dau ----------

    @GetMapping("/skin-quiz/welcome")
    public String welcome(Authentication auth, HttpServletRequest request, Model model) {
        User user = currentUser(auth);
        if (user == null) {
            return "redirect:/login";
        }
        // Vua dang nhap sau khi lam trac nghiem luc con la khach: hien thang
        // ket qua vua lam, khong hoi lam lai trac nghiem khac.
        GuestRoutineSession guest = guestSession(request);
        if (guest != null) {
            skinService.migrateGuestRoutine(user, guest);
            request.getSession().removeAttribute(GuestRoutineSession.SESSION_KEY);
            return "redirect:/my-routine";
        }
        model.addAttribute("hasRoutine", routines.findByUserId(user.getId()).isPresent());
        model.addAttribute("fullName", user.getFullName());
        return "skin-quiz/welcome";
    }

    @PostMapping("/skin-quiz/welcome")
    public String welcomeSubmit(Authentication auth,
                                HttpServletRequest request,
                                @RequestParam String action,
                                RedirectAttributes redirect) {
        User user = currentUser(auth);
        if (user == null) {
            return "redirect:/login";
        }
        if ("later".equals(action)) {
            // Dang co ket qua tam cua khach: luu no xuong DB thay vi bo qua.
            GuestRoutineSession guest = guestSession(request);
            if (guest != null) {
                skinService.migrateGuestRoutine(user, guest);
                request.getSession().removeAttribute(GuestRoutineSession.SESSION_KEY);
                return "redirect:/my-routine";
            }
            user.setSkinQuizAsked(true);
            users.save(user);
            redirect.addFlashAttribute("quizSkipped",
                    "Không sao đâu, bạn có thể làm trắc nghiệm da bất cứ lúc nào tại mục Chu trình chăm sóc da.");
            return "redirect:/";
        }
        return "redirect:/skin-quiz";
    }

    // ---------- Trang trac nghiem ----------

    @GetMapping("/skin-quiz")
    public String quiz(Model model) {
        SkinQuizDataLoader.QuizData quiz = quizLoader.loadActive();
        model.addAttribute("quizId", quiz.quizId());
        model.addAttribute("questions", quiz.questions());
        model.addAttribute("goals", SkinQuizService.GOALS);
        model.addAttribute("budgets", SkinQuizService.BUDGETS);
        model.addAttribute("skinInfos", SkinQuizService.SKIN_INFOS);
        return "skin-quiz/quiz";
    }

    @PostMapping("/skin-quiz/submit")
    public String submit(Authentication auth,
                         HttpServletRequest request,
                         @RequestParam int quizId,
                         @RequestParam(required = false) List<String> goals,
                         @RequestParam(required = false) String budget,
                         Model model,
                         RedirectAttributes redirect) {
        // Doc dap an theo ten rieng tung cau (answer1..answerN, dung thu tu
        // cau hoi trong DB). Tuong thich nguoc: neu form cu gui chung
        // name="answers" thi dung danh sach do.
        int questionCount = quizLoader.loadActive().questions().size();
        if (questionCount <= 0) {
            questionCount = 5;
        }
        List<String> answers = new java.util.ArrayList<>();
        String[] legacy = request.getParameterValues("answers");
        if (legacy != null && legacy.length == questionCount) {
            answers.addAll(List.of(legacy));
        } else {
            for (int i = 1; i <= questionCount; i++) {
                String value = request.getParameter("answer" + i);
                if (value != null && !value.isBlank()) {
                    answers.add(value);
                }
            }
        }
        // Kiem tra lai phia server (JS chi la tien ich, khong phai bao mat).
        if (answers.size() != questionCount) {
            model.addAttribute("error", "Bạn còn thiếu " + (questionCount - answers.size())
                    + " câu hỏi về làn da. Bạn quay lại các câu chưa chọn rồi nộp lại nhé.");
            return quiz(model);
        }
        if (goals == null || goals.isEmpty() || goals.size() > 3
                || goals.stream().anyMatch(g -> !skinService.validGoal(g))) {
            model.addAttribute("error", "Bạn chọn 1–3 mục tiêu chăm sóc da nhé.");
            return quiz(model);
        }
        if (!skinService.validBudget(budget)) {
            model.addAttribute("error", "Bạn chọn 1 mức ngân sách nhé.");
            return quiz(model);
        }
        // Khach (chua dang nhap): chi luu tam ket qua vao session, khong ghi DB.
        // Customer/Admin: luu ket qua + chu trinh xuong DB nhu cu.
        User user = currentUser(auth);
        if (user == null) {
            SkinRoutine tmp = skinService.buildTransientRoutine(quizId, answers, goals, budget);
            request.getSession().setAttribute(GuestRoutineSession.SESSION_KEY,
                    new GuestRoutineSession(quizId, tmp.getSkinType(), tmp.getGoals(),
                            tmp.getBudget(), tmp.getRoutineJson()));
            return "redirect:/my-routine/building";
        }
        skinService.saveQuizAndRoutine(user, quizId, answers, goals, budget);
        // Bai moi vua lam khi da dang nhap thang ket qua tam cu trong session
        // (neu con): neu khong xoa, lan mo /my-routine ke tiep se lay ket qua
        // cu ghi de len ket qua moi vua luu -> lam lai van khong thay doi.
        request.getSession().removeAttribute(GuestRoutineSession.SESSION_KEY);
        return "redirect:/my-routine/building";
    }

    // ---------- Man hinh "dang thiet lap" ----------

    @GetMapping("/my-routine/building")
    public String building(Authentication auth, HttpServletRequest request, Model model) {
        ResolvedRoutine resolved = resolveRoutine(auth, request);
        if (resolved == null) {
            return "redirect:/skin-quiz";
        }
        SkinRoutine routine = resolved.routine();
        model.addAttribute("skin", skinService.skinInfo(routine.getSkinType()));
        model.addAttribute("goalLabels", skinService.goalLabels(
                routine.getGoals() != null && !routine.getGoals().isBlank()
                        ? List.of(routine.getGoals().split(",")) : List.of()));
        model.addAttribute("budgetLabel", skinService.budgetLabel(routine.getBudget()));
        model.addAttribute("isGuest", resolved.isGuest());
        return "skin-quiz/building";
    }

    // ---------- Trang chu trinh ----------

    @GetMapping("/my-routine")
    public String routine(Authentication auth, HttpServletRequest request, Model model) {
        ResolvedRoutine resolved = resolveRoutine(auth, request);
        if (resolved == null) {
            return currentUser(auth) != null ? "redirect:/skin-quiz/welcome" : "redirect:/skin-quiz";
        }
        SkinRoutine routine = resolved.routine();
        boolean isGuest = resolved.isGuest();
        List<RoutineStep> steps = skinService.loadSteps(routine);
        List<RoutineStep> morning = steps.stream()
                .filter(s -> "MORNING".equals(s.getTime())).toList();
        List<RoutineStep> evening = steps.stream()
                .filter(s -> "EVENING".equals(s.getTime())).toList();
        model.addAttribute("skin", skinService.skinInfo(routine.getSkinType()));
        model.addAttribute("goalLabels", skinService.goalLabels(
                routine.getGoals() != null && !routine.getGoals().isBlank()
                        ? List.of(routine.getGoals().split(",")) : List.of()));
        model.addAttribute("budgetLabel", skinService.budgetLabel(routine.getBudget()));
        model.addAttribute("morningSteps", morning);
        model.addAttribute("eveningSteps", evening);
        model.addAttribute("allSteps", steps);
        BigDecimal total = skinService.totalPrice(steps);
        model.addAttribute("totalPrice", total);
        model.addAttribute("totalPriceText",
                java.text.NumberFormat.getInstance(new java.util.Locale("vi", "VN")).format(total) + "đ");
        // Lich day du la 7 buoc (sang 3 + toi 4). Thieu buoc = variant ngung ban.
        model.addAttribute("missingSteps", 7 - steps.size());
        model.addAttribute("days", List.of("Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "Chủ nhật"));
        model.addAttribute("isGuest", isGuest);
        return "skin-quiz/routine";
    }

    // ---------- Mua tat ca san pham trong chu trinh ----------

    @PostMapping("/my-routine/buy-all")
    public String buyAll(Authentication auth, HttpServletRequest request) {
        ResolvedRoutine resolved = resolveRoutine(auth, request);
        if (resolved == null) {
            return "redirect:/skin-quiz";
        }
        // Moi san pham chi mua 1 lan du dung ca sang lan toi.
        List<Integer> variantIds = skinService.loadSteps(resolved.routine()).stream()
                .map(step -> step.getCard().getVariant().getId())
                .distinct().toList();
        User user = currentUser(auth);
        if (user != null) {
            variantIds.forEach(id -> cartService.addItem(user, id, 1));
        } else {
            variantIds.forEach(id -> cartService.addGuestItem(request.getSession(), id, 1));
        }
        return "redirect:/cart";
    }

    // ---------- Doi san pham tuong tu ----------

    @PostMapping("/my-routine/swap")
    public String swap(Authentication auth,
                       @RequestParam int stepIndex,
                       @RequestParam int variantId,
                       RedirectAttributes redirect) {
        User user = currentUser(auth);
        if (user == null) {
            return "redirect:/login";
        }
        Optional<SkinRoutine> routineOpt = routines.findByUserId(user.getId());
        if (routineOpt.isEmpty()) {
            return "redirect:/skin-quiz/welcome";
        }
        boolean ok = skinService.swapStep(routineOpt.get(), stepIndex, variantId);
        redirect.addFlashAttribute(ok ? "swapped" : "swapError",
                ok ? "Đã đổi sản phẩm trong chu trình của bạn."
                        : "Không đổi được sản phẩm này (chỉ đổi sang sản phẩm cùng danh mục, đang bán và còn hàng).");
        return "redirect:/my-routine";
    }
}
