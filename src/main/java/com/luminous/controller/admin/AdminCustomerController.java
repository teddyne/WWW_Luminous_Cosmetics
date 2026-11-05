package com.luminous.controller.admin;

import com.luminous.dto.CustomerCreateForm;
import com.luminous.dto.ProfileForm;
import com.luminous.security.CustomUserDetails;
import com.luminous.service.admin.AdminCustomerService;
import java.math.BigDecimal;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/admin/customers")
public class AdminCustomerController {

    private final AdminCustomerService service;

    public AdminCustomerController(AdminCustomerService service) {
        this.service = service;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) String role,
                       @RequestParam(defaultValue = "newest") String sort,
                       @RequestParam(required = false) BigDecimal minSpent,
                       @RequestParam(required = false) Integer minOrders,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       Model model) {
        String effectiveRole = "ROLE_ADMIN".equals(role) ? "ROLE_ADMIN" : "ROLE_CUSTOMER";
        model.addAttribute("customers", service.page(q, effectiveRole, null, sort, minSpent, minOrders, page, size));
        model.addAttribute("q", q);
        model.addAttribute("role", effectiveRole);
        model.addAttribute("sort", sort);
        model.addAttribute("minSpent", minSpent);
        model.addAttribute("minOrders", minOrders);
        model.addAttribute("baseUrl", "/admin/customers");
        model.addAttribute("pageQuery", AdminPageLinks.query(
                "q", q, "role", effectiveRole, "sort", sort, "minSpent", minSpent, "minOrders", minOrders));
        model.addAttribute("activeMenu", "ROLE_ADMIN".equals(effectiveRole) ? "admins" : "customers");
        return "admin/customers";
    }

    @GetMapping("/new")
    public String createForm(@RequestParam(required = false) String role,
                             Model model) {
        if (!model.containsAttribute("customerForm")) {
            CustomerCreateForm form = new CustomerCreateForm();
            form.setRoleName("ROLE_ADMIN".equals(role) ? "ROLE_ADMIN" : "ROLE_CUSTOMER");
            model.addAttribute("customerForm", form);
        }
        model.addAttribute("formTitle",
                "ROLE_ADMIN".equals(role) ? "Thêm quản trị viên" : "Thêm khách hàng");
        model.addAttribute("activeMenu", "customers");
        return "admin/customer-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("customerForm") CustomerCreateForm form,
                         BindingResult result,
                         Model model,
                         RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("formTitle",
                    "ROLE_ADMIN".equals(form.getRoleName()) ? "Thêm quản trị viên" : "Thêm khách hàng");
            model.addAttribute("activeMenu", "customers");
            return "admin/customer-form";
        }
        try {
            Integer id = service.create(form);
            redirect.addFlashAttribute("toast", "Đã thêm tài khoản mới");
            redirect.addFlashAttribute("toastType", "success");
            return "redirect:/admin/customers/" + id;
        } catch (IllegalArgumentException e) {
            model.addAttribute("formError", e.getMessage());
            model.addAttribute("formTitle",
                    "ROLE_ADMIN".equals(form.getRoleName()) ? "Thêm quản trị viên" : "Thêm khách hàng");
            model.addAttribute("activeMenu", "customers");
            return "admin/customer-form";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        var customer = service.detailRow(id);
        model.addAttribute("customer", customer);
        model.addAttribute("orders", service.ordersOf(id));
        model.addAttribute("reviews", service.reviewsOf(id));
        if (!model.containsAttribute("profileForm")) {
            ProfileForm form = new ProfileForm();
            form.setFullName(customer.fullName());
            form.setPhone(customer.phone());
            model.addAttribute("profileForm", form);
        }
        model.addAttribute("activeMenu", "customers");
        return "admin/customer-detail";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Integer id,
                         @AuthenticationPrincipal CustomUserDetails admin,
                         RedirectAttributes redirect) {
        try {
            service.toggleActive(id, admin.getUser().getId());
            redirect.addFlashAttribute("toast", "Đã đổi trạng thái tài khoản");
            redirect.addFlashAttribute("toastType", "success");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("toast", e.getMessage());
            redirect.addFlashAttribute("toastType", "error");
        }
        return "redirect:/admin/customers/" + id;
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Integer id,
                         @Valid @ModelAttribute("profileForm") ProfileForm form,
                         BindingResult result,
                         @RequestParam String roleName,
                         @AuthenticationPrincipal CustomUserDetails admin,
                         Model model,
                         RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return detail(id, model);
        }
        try {
            service.update(id, form.getFullName(), form.getPhone(), roleName, admin.getUser().getId());
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("toast", e.getMessage());
            redirect.addFlashAttribute("toastType", "error");
            return "redirect:/admin/customers/" + id;
        }
        redirect.addFlashAttribute("toast", "Đã cập nhật tài khoản");
        redirect.addFlashAttribute("toastType", "success");
        return "redirect:/admin/customers/" + id;
    }

    @PostMapping("/bulk")
    public String bulk(@RequestParam(required = false) java.util.List<Integer> ids,
                       @RequestParam String bulkAction,
                       @AuthenticationPrincipal CustomUserDetails admin,
                       RedirectAttributes redirect) {
        if (ids == null || ids.isEmpty()) {
            redirect.addFlashAttribute("toast", "Chưa chọn khách hàng nào");
            redirect.addFlashAttribute("toastType", "error");
            return "redirect:/admin/customers";
        }
        Integer adminId = admin == null ? null : admin.getUser().getId();
        switch (bulkAction) {
            case "lock" -> redirect.addFlashAttribute("toast",
                    "Đã khóa " + service.bulkSetActive(ids, false, adminId) + " tài khoản");
            case "unlock" -> redirect.addFlashAttribute("toast",
                    "Đã mở khóa " + service.bulkSetActive(ids, true, adminId) + " tài khoản");
            default -> {
                redirect.addFlashAttribute("toast", "Thao tác không hợp lệ");
                redirect.addFlashAttribute("toastType", "error");
                return "redirect:/admin/customers";
            }
        }
        redirect.addFlashAttribute("toastType", "success");
        return "redirect:/admin/customers";
    }
}
