package com.luminous.controller.admin;

import com.luminous.dto.CategoryForm;
import com.luminous.service.admin.AdminCategoryService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/categories")
public class AdminCategoryController {

    private final AdminCategoryService service;

    public AdminCategoryController(AdminCategoryService service) {
        this.service = service;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) String status,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       Model model) {
        model.addAttribute("categories", service.page(q, status, page, size));
        if (!model.containsAttribute("categoryForm")) {
            model.addAttribute("categoryForm", new CategoryForm());
        }
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("baseUrl", "/admin/categories");
        model.addAttribute("pageQuery", AdminPageLinks.query("q", q, "status", status));
        model.addAttribute("activeMenu", "categories");
        return "admin/categories";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("categoryForm") CategoryForm form,
                         BindingResult result,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "10") int size,
                         @RequestParam(required = false) String q,
                         @RequestParam(required = false) String status,
                         Model model,
                         RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("showCategoryModal", true);
            model.addAttribute("categoryModalAction", "/admin/categories");
            return list(q, status, page, size, model);
        }
        try {
            service.create(form);
        } catch (IllegalArgumentException e) {
            result.rejectValue("name", "duplicate", e.getMessage());
            model.addAttribute("showCategoryModal", true);
            model.addAttribute("categoryModalAction", "/admin/categories");
            return list(q, status, page, size, model);
        }
        redirect.addFlashAttribute("toast", "Đã thêm loại sản phẩm mới");
        redirect.addFlashAttribute("toastType", "success");
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Integer id,
                         @Valid @ModelAttribute("categoryForm") CategoryForm form,
                         BindingResult result,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "10") int size,
                         @RequestParam(required = false) String q,
                         @RequestParam(required = false) String status,
                         Model model,
                         RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("showCategoryModal", true);
            model.addAttribute("categoryModalAction", "/admin/categories/" + id);
            return list(q, status, page, size, model);
        }
        try {
            service.update(id, form);
        } catch (IllegalArgumentException e) {
            result.rejectValue("name", "duplicate", e.getMessage());
            model.addAttribute("showCategoryModal", true);
            model.addAttribute("categoryModalAction", "/admin/categories/" + id);
            return list(q, status, page, size, model);
        }
        redirect.addFlashAttribute("toast", "Đã cập nhật loại sản phẩm");
        redirect.addFlashAttribute("toastType", "success");
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Integer id, RedirectAttributes redirect) {
        service.toggleStatus(id);
        redirect.addFlashAttribute("toast", "Đã đổi trạng thái loại sản phẩm");
        redirect.addFlashAttribute("toastType", "success");
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Integer id, RedirectAttributes redirect) {
        try {
            service.delete(id);
            redirect.addFlashAttribute("toast", "Đã xóa loại sản phẩm");
            redirect.addFlashAttribute("toastType", "success");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("toast", e.getMessage());
            redirect.addFlashAttribute("toastType", "error");
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/bulk")
    public String bulk(@RequestParam(required = false) java.util.List<Integer> ids,
                       RedirectAttributes redirect) {
        if (ids == null || ids.isEmpty()) {
            redirect.addFlashAttribute("toast", "Chưa chọn loại sản phẩm nào");
            redirect.addFlashAttribute("toastType", "error");
            return "redirect:/admin/categories";
        }
        int[] done = service.bulkDelete(ids);
        redirect.addFlashAttribute("toast", "Đã xóa " + done[0] + " loại sản phẩm"
                + (done[1] > 0 ? ", bỏ qua " + done[1] + " loại còn sản phẩm" : ""));
        redirect.addFlashAttribute("toastType", "success");
        return "redirect:/admin/categories";
    }
}
