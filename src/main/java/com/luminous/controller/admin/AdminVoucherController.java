package com.luminous.controller.admin;

import com.luminous.dto.VoucherForm;
import com.luminous.service.admin.AdminVoucherService;
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
import java.time.LocalDateTime;

@Controller
@RequestMapping("/admin/vouchers")
public class AdminVoucherController {

    private final AdminVoucherService service;

    public AdminVoucherController(AdminVoucherService service) {
        this.service = service;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) Boolean active,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       Model model) {
        model.addAttribute("vouchers", service.page(q, active, page, size));
        model.addAttribute("q", q);
        model.addAttribute("active", active);
        model.addAttribute("baseUrl", "/admin/vouchers");
        model.addAttribute("pageQuery", AdminPageLinks.query("q", q, "active", active));
        model.addAttribute("activeMenu", "vouchers");
        return "admin/vouchers";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        if (!model.containsAttribute("voucherForm")) {
            VoucherForm form = new VoucherForm();
            form.setStartDate(LocalDateTime.now().withSecond(0).withNano(0));
            form.setEndDate(LocalDateTime.now().plusDays(30).withSecond(0).withNano(0));
            model.addAttribute("voucherForm", form);
        }
        model.addAttribute("categories", service.categories());
        model.addAttribute("products", service.products());
        model.addAttribute("formTitle", "Thêm khuyến mãi mới");
        model.addAttribute("formAction", "/admin/vouchers");
        model.addAttribute("activeMenu", "vouchers");
        return "admin/voucher-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("voucherForm") VoucherForm form,
                         BindingResult result,
                         Model model,
                         RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return createForm(model);
        }
        try {
            service.create(form);
        } catch (IllegalArgumentException e) {
            model.addAttribute("formError", e.getMessage());
            return createForm(model);
        }
        redirect.addFlashAttribute("toast", "Đã thêm khuyến mãi mới");
        redirect.addFlashAttribute("toastType", "success");
        return "redirect:/admin/vouchers";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Integer id, Model model) {
        if (!model.containsAttribute("voucherForm")) {
            model.addAttribute("voucherForm", service.formFor(id));
        }
        model.addAttribute("categories", service.categories());
        model.addAttribute("products", service.products());
        model.addAttribute("formTitle", "Chỉnh sửa khuyến mãi");
        model.addAttribute("formAction", "/admin/vouchers/" + id);
        model.addAttribute("activeMenu", "vouchers");
        return "admin/voucher-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Integer id,
                         @Valid @ModelAttribute("voucherForm") VoucherForm form,
                         BindingResult result,
                         Model model,
                         RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return editForm(id, model);
        }
        try {
            service.update(id, form);
        } catch (IllegalArgumentException e) {
            model.addAttribute("formError", e.getMessage());
            return editForm(id, model);
        }
        redirect.addFlashAttribute("toast", "Đã cập nhật khuyến mãi");
        redirect.addFlashAttribute("toastType", "success");
        return "redirect:/admin/vouchers";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Integer id, RedirectAttributes redirect) {
        service.toggleActive(id);
        redirect.addFlashAttribute("toast", "Đã đổi trạng thái khuyến mãi");
        redirect.addFlashAttribute("toastType", "success");
        return "redirect:/admin/vouchers";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Integer id, RedirectAttributes redirect) {
        try {
            service.delete(id);
            redirect.addFlashAttribute("toast", "Đã xóa khuyến mãi");
            redirect.addFlashAttribute("toastType", "success");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("toast", e.getMessage());
            redirect.addFlashAttribute("toastType", "error");
        }
        return "redirect:/admin/vouchers";
    }

    @PostMapping("/bulk")
    public String bulk(@RequestParam(required = false) java.util.List<Integer> ids,
                       @RequestParam String bulkAction,
                       RedirectAttributes redirect) {
        if (ids == null || ids.isEmpty()) {
            redirect.addFlashAttribute("toast", "Chưa chọn khuyến mãi nào");
            redirect.addFlashAttribute("toastType", "error");
            return "redirect:/admin/vouchers";
        }
        switch (bulkAction) {
            case "delete" -> {
                int[] done = service.bulkDelete(ids);
                redirect.addFlashAttribute("toast", "Đã xóa " + done[0] + " khuyến mãi"
                        + (done[1] > 0 ? ", bỏ qua " + done[1] + " khuyến mãi đã được dùng" : ""));
            }
            case "deactivate" -> redirect.addFlashAttribute("toast",
                    "Đã tạm dừng " + service.bulkSetActive(ids, false) + " khuyến mãi");
            case "activate" -> redirect.addFlashAttribute("toast",
                    "Đã kích hoạt " + service.bulkSetActive(ids, true) + " khuyến mãi");
            default -> {
                redirect.addFlashAttribute("toast", "Thao tác không hợp lệ");
                redirect.addFlashAttribute("toastType", "error");
                return "redirect:/admin/vouchers";
            }
        }
        redirect.addFlashAttribute("toastType", "success");
        return "redirect:/admin/vouchers";
    }
}
