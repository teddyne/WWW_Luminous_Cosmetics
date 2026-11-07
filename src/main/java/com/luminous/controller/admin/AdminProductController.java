package com.luminous.controller.admin;

import com.luminous.dto.ProductForm;
import com.luminous.service.admin.AdminProductService;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/products")
public class AdminProductController {

    private final AdminProductService service;

    public AdminProductController(AdminProductService service) {
        this.service = service;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) Integer categoryId,
                       @RequestParam(required = false) Boolean active,
                       @RequestParam(defaultValue = "newest") String sort,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {
        model.addAttribute("products", service.page(q, categoryId, active, sort, page, size));
        model.addAttribute("categories", service.categories());
        model.addAttribute("q", q);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("active", active);
        model.addAttribute("sort", sort);
        model.addAttribute("baseUrl", "/admin/products");
        model.addAttribute("pageQuery", AdminPageLinks.query(
                "q", q, "categoryId", categoryId, "active", active, "sort", sort));
        model.addAttribute("activeMenu", "products");
        return "admin/products";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        if (!model.containsAttribute("productForm")) {
            model.addAttribute("productForm", new ProductForm());
        }
        model.addAttribute("categories", service.categories());
        model.addAttribute("skinTypes", AdminProductService.SKIN_TYPES);
        model.addAttribute("formTitle", "Thêm sản phẩm mới");
        model.addAttribute("formAction", "/admin/products");
        model.addAttribute("activeMenu", "products");
        return "admin/product-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("productForm") ProductForm form,
                         BindingResult result,
                         @RequestParam(defaultValue = "publish") String action,
                         @RequestParam(value = "image", required = false) MultipartFile image,
                         @RequestParam(value = "gallery", required = false) MultipartFile[] gallery,
                         Model model,
                         RedirectAttributes redirect) {
        form.setActive(!"draft".equals(action));
        if (result.hasErrors()) {
            return createForm(model);
        }
        try {
            Integer id = service.create(form, image, gallery);
            redirect.addFlashAttribute("toast", "Đã thêm sản phẩm mới");
            redirect.addFlashAttribute("toastType", "success");
            return "redirect:/admin/products/" + id + "/edit";
        } catch (IllegalArgumentException e) {
            model.addAttribute("formError", e.getMessage());
            return createForm(model);
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Integer id, Model model) {
        if (!model.containsAttribute("productForm")) {
            model.addAttribute("productForm", service.formFor(id));
        }
        model.addAttribute("categories", service.categories());
        model.addAttribute("skinTypes", AdminProductService.SKIN_TYPES);
        model.addAttribute("formTitle", "Chỉnh sửa sản phẩm");
        model.addAttribute("formAction", "/admin/products/" + id);
        model.addAttribute("activeMenu", "products");
        return "admin/product-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Integer id,
                         @Valid @ModelAttribute("productForm") ProductForm form,
                         BindingResult result,
                         @RequestParam(defaultValue = "publish") String action,
                         @RequestParam(value = "image", required = false) MultipartFile image,
                         @RequestParam(value = "gallery", required = false) MultipartFile[] gallery,
                         Model model,
                         RedirectAttributes redirect) {
        form.setActive(!"draft".equals(action));
        if (result.hasErrors()) {
            return editForm(id, model);
        }
        try {
            service.update(id, form, image, gallery);
        } catch (IllegalArgumentException e) {
            model.addAttribute("formError", e.getMessage());
            return editForm(id, model);
        }
        redirect.addFlashAttribute("toast", "Đã cập nhật sản phẩm");
        redirect.addFlashAttribute("toastType", "success");
        return "redirect:/admin/products";
    }

    @PostMapping("/bulk")
    public String bulk(@RequestParam(required = false) java.util.List<Integer> ids,
                       @RequestParam String bulkAction,
                       RedirectAttributes redirect) {
        if (ids == null || ids.isEmpty()) {
            redirect.addFlashAttribute("toast", "Chưa chọn sản phẩm nào");
            redirect.addFlashAttribute("toastType", "error");
            return "redirect:/admin/products";
        }
        switch (bulkAction) {
            case "delete" -> {
                int[] done = service.bulkDelete(ids);
                redirect.addFlashAttribute("toast", "Đã xóa " + done[0] + " sản phẩm"
                        + (done[1] > 0 ? ", bỏ qua " + done[1] + " sản phẩm đã có đơn hàng" : ""));
            }
            case "deactivate" -> redirect.addFlashAttribute("toast",
                    "Đã ngừng bán " + service.bulkSetActive(ids, false) + " sản phẩm");
            case "activate" -> redirect.addFlashAttribute("toast",
                    "Đã mở bán " + service.bulkSetActive(ids, true) + " sản phẩm");
            default -> {
                redirect.addFlashAttribute("toast", "Thao tác không hợp lệ");
                redirect.addFlashAttribute("toastType", "error");
                return "redirect:/admin/products";
            }
        }
        redirect.addFlashAttribute("toastType", "success");
        return "redirect:/admin/products";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Integer id, RedirectAttributes redirect) {
        service.toggleActive(id);
        redirect.addFlashAttribute("toast", "Đã đổi trạng thái sản phẩm");
        redirect.addFlashAttribute("toastType", "success");
        return "redirect:/admin/products";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Integer id, RedirectAttributes redirect) {
        try {
            service.delete(id);
            redirect.addFlashAttribute("toast", "Đã xóa sản phẩm");
            redirect.addFlashAttribute("toastType", "success");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("toast", e.getMessage());
            redirect.addFlashAttribute("toastType", "error");
        }
        return "redirect:/admin/products";
    }
}
