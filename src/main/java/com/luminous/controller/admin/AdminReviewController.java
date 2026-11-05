package com.luminous.controller.admin;

import com.luminous.service.admin.AdminReviewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/reviews")
public class AdminReviewController {

    private final AdminReviewService service;

    public AdminReviewController(AdminReviewService service) {
        this.service = service;
    }

    @GetMapping
    public String list(@RequestParam(required = false) Byte rating,
                       @RequestParam(required = false) Boolean visible,
                       @RequestParam(required = false) String q,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       Model model) {
        model.addAttribute("reviews", service.page(rating, visible, q, page, size));
        model.addAttribute("rating", rating);
        model.addAttribute("visible", visible);
        model.addAttribute("q", q);
        model.addAttribute("baseUrl", "/admin/reviews");
        model.addAttribute("pageQuery", AdminPageLinks.query(
                "rating", rating, "visible", visible, "q", q));
        model.addAttribute("activeMenu", "reviews");
        return "admin/reviews";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Integer id, RedirectAttributes redirect) {
        service.toggleVisible(id);
        redirect.addFlashAttribute("toast", "Đã đổi trạng thái hiển thị đánh giá");
        redirect.addFlashAttribute("toastType", "success");
        return "redirect:/admin/reviews";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Integer id, RedirectAttributes redirect) {
        service.delete(id);
        redirect.addFlashAttribute("toast", "Đã xóa đánh giá");
        redirect.addFlashAttribute("toastType", "success");
        return "redirect:/admin/reviews";
    }

    @PostMapping("/bulk")
    public String bulk(@RequestParam(required = false) java.util.List<Integer> ids,
                       @RequestParam String bulkAction,
                       RedirectAttributes redirect) {
        if (ids == null || ids.isEmpty()) {
            redirect.addFlashAttribute("toast", "Chưa chọn đánh giá nào");
            redirect.addFlashAttribute("toastType", "error");
            return "redirect:/admin/reviews";
        }
        switch (bulkAction) {
            case "delete" -> redirect.addFlashAttribute("toast",
                    "Đã xóa " + service.bulkDelete(ids) + " đánh giá");
            case "hide" -> redirect.addFlashAttribute("toast",
                    "Đã ẩn " + service.bulkSetVisible(ids, false) + " đánh giá");
            case "show" -> redirect.addFlashAttribute("toast",
                    "Đã hiện " + service.bulkSetVisible(ids, true) + " đánh giá");
            default -> {
                redirect.addFlashAttribute("toast", "Thao tác không hợp lệ");
                redirect.addFlashAttribute("toastType", "error");
                return "redirect:/admin/reviews";
            }
        }
        redirect.addFlashAttribute("toastType", "success");
        return "redirect:/admin/reviews";
    }
}
