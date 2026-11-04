package com.luminous.controller;

import com.luminous.dto.RoutineStep;
import com.luminous.model.SkinRoutine;
import com.luminous.model.User;
import com.luminous.repository.SkinRoutineRepository;
import com.luminous.repository.UserRepository;
import com.luminous.security.CustomUserDetails;
import com.luminous.service.SkinQuizDataLoader;
import com.luminous.service.SkinQuizService;
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

/**
 * Luong trac nghiem da + chu trinh:
 *   Dang nhap lan dau -> /skin-quiz/welcome (hoi co lam khong)
 *     -> Co -> /skin-quiz (buoc 1 muc tieu, buoc 2 ngan sach,
 *        buoc 3-7 la 5 cau DB ve da)
 *     -> Nop -> /my-routine/building (man hinh "dang thiet lap...")
 *     -> /my-routine (lich sang/toi, lich tuan, chi tiet, doi san pham)
 *     -> De sau -> / (khong hoi lai o lan dang nhap sau, van vao lai
 *        duoc bang /skin-quiz hoac /my-routine).
 * Server kiem tra lai moi dieu kien ma JS da kiem tra: du dap an
 * (so cau lay tu DB, hien la 5), 1-3 muc tieu, ngan sach hop le,
 * doi san pham cung danh muc.
 * SUA LOI BAN CU: dap an moi cau co name rieng answer1..answerN
 * (xem quiz.html). Ban cu dung chung name="answers" cho ca 5 cau nen
 * trinh duyet coi la 1 nhom radio, chon cau sau tu bo chon cau truoc.
 */
@Controller
public class SkinQuizController {
    private final SkinQuizDataLoader quizLoader;
    private final SkinQuizService skinService;
    private final SkinRoutineRepository routines;
    private final UserRepository users;

    public SkinQuizController(SkinQuizDataLoader quizLoader,
                              SkinQuizService skinService,
                              SkinRoutineRepository routines,
                              UserRepository users) {
        this.quizLoader = quizLoader;
        this.skinService = skinService;
        this.routines = routines;
        this.users = users;
    }

    private User currentUser(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails details) {
            return details.getUser();
        }
        return null;
    }

    // ---------- Trang hoi lan dau ----------

    @GetMapping("/skin-quiz/welcome")
    public String welcome(Authentication auth, Model model) {
        User user = currentUser(auth);
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("hasRoutine", routines.findByUserId(user.getId()).isPresent());
        model.addAttribute("fullName", user.getFullName());
        return "skin-quiz/welcome";
    }

    @PostMapping("/skin-quiz/welcome")
    public String welcomeSubmit(Authentication auth,
                                @RequestParam String action,
                                RedirectAttributes redirect) {
        User user = currentUser(auth);
        if (user == null) {
            return "redirect:/login";
        }
        if ("later".equals(action)) {
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
        User user = currentUser(auth);
        if (user == null) {
            return "redirect:/login";
        }
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
        skinService.saveQuizAndRoutine(user, quizId, answers, goals, budget);
        return "redirect:/my-routine/building";
    }

    // ---------- Man hinh "dang thiet lap" ----------

    @GetMapping("/my-routine/building")
    public String building(Authentication auth, Model model) {
        User user = currentUser(auth);
        if (user == null) {
            return "redirect:/login";
        }
        Optional<SkinRoutine> routine = routines.findByUserId(user.getId());
        if (routine.isEmpty()) {
            return "redirect:/skin-quiz";
        }
        SkinRoutine r = routine.get();
        model.addAttribute("skin", skinService.skinInfo(r.getSkinType()));
        model.addAttribute("goalLabels", skinService.goalLabels(
                r.getGoals() != null && !r.getGoals().isBlank()
                        ? List.of(r.getGoals().split(",")) : List.of()));
        model.addAttribute("budgetLabel", skinService.budgetLabel(r.getBudget()));
        return "skin-quiz/building";
    }

    // ---------- Trang chu trinh ----------

    @GetMapping("/my-routine")
    public String routine(Authentication auth, Model model) {
        User user = currentUser(auth);
        if (user == null) {
            return "redirect:/login";
        }
        Optional<SkinRoutine> routineOpt = routines.findByUserId(user.getId());
        if (routineOpt.isEmpty()) {
            return "redirect:/skin-quiz/welcome";
        }
        SkinRoutine routine = routineOpt.get();
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
        return "skin-quiz/routine";
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
