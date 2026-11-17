package com.luminous.controller.admin;

import com.luminous.dto.FlashSaleForm;
import com.luminous.model.FlashSale;
import com.luminous.model.enums.FlashSaleStatus;
import com.luminous.service.flashsale.FlashSaleService;
import jakarta.validation.Valid;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
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
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/flash-sales")
public class AdminFlashSaleController {

    private final FlashSaleService service;

    public AdminFlashSaleController(FlashSaleService service) {
        this.service = service;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) String display,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {
        Page<FlashSale> result = service.search(q, display, page, size);
        List<FlashSale> sales = result.getContent();
        model.addAttribute("page", result);
        model.addAttribute("sales", sales);
        model.addAttribute("now", LocalDateTime.now());
        Map<Integer, Integer> itemCounts = new HashMap<>();
        for (FlashSale sale : sales) {
            itemCounts.put(sale.getId(), service.itemsOf(sale.getId()).size());
        }
        model.addAttribute("itemCounts", itemCounts);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("display", display == null ? "" : display);
        model.addAttribute("baseUrl", "/admin/flash-sales");
        model.addAttribute("pageQuery", AdminPageLinks.query("q", q, "display", display));
        model.addAttribute("activeMenu", "flash-sales");
        return "admin/flash-sales";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        if (!model.containsAttribute("flashSaleForm")) {
            model.addAttribute("flashSaleForm", service.blankForm());
        }
        model.addAttribute("products", service.productOptions());
        model.addAttribute("flatVariants", service.flatVariants());
        model.addAttribute("flashBrands", service.brands());
        model.addAttribute("formTitle", "Thêm chương trình Flash Sale");
        model.addAttribute("formAction", "/admin/flash-sales");
        model.addAttribute("activeMenu", "flash-sales");
        return "admin/flash-sale-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("flashSaleForm") FlashSaleForm form,
                         BindingResult result,
                         @RequestParam(value = "banner", required = false) MultipartFile banner,
                         Model model,
                         RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return createForm(model);
        }
        try {
            service.create(form, banner);
        } catch (IllegalArgumentException e) {
            model.addAttribute("formError", e.getMessage());
            return createForm(model);
        }
        redirect.addFlashAttribute("toast", "Đã thêm chương trình Flash Sale");
        redirect.addFlashAttribute("toastType", "success");
        return "redirect:/admin/flash-sales";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Integer id, Model model) {
        if (!model.containsAttribute("flashSaleForm")) {
            model.addAttribute("flashSaleForm", service.formFor(id));
        }
        model.addAttribute("products", service.productOptions());
        model.addAttribute("flatVariants", service.flatVariants());
        model.addAttribute("flashBrands", service.brands());
        model.addAttribute("formTitle", "Chỉnh sửa chương trình Flash Sale");
        model.addAttribute("formAction", "/admin/flash-sales/" + id);
        model.addAttribute("activeMenu", "flash-sales");
        return "admin/flash-sale-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Integer id,
                         @Valid @ModelAttribute("flashSaleForm") FlashSaleForm form,
                         BindingResult result,
                         @RequestParam(value = "banner", required = false) MultipartFile banner,
                         Model model,
                         RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return editForm(id, model);
        }
        try {
            service.update(id, form, banner);
        } catch (IllegalArgumentException e) {
            model.addAttribute("formError", e.getMessage());
            return editForm(id, model);
        } catch (OptimisticLockingFailureException e) {
            model.addAttribute("formError", "Chương trình vừa được người khác sửa, vui lòng tải lại rồi sửa tiếp.");
            model.addAttribute("versionConflict", true);
            return editForm(id, model);
        }
        redirect.addFlashAttribute("toast", "Đã cập nhật chương trình Flash Sale");
        redirect.addFlashAttribute("toastType", "success");
        return "redirect:/admin/flash-sales";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Integer id, RedirectAttributes redirect) {
        FlashSale sale = service.getOrThrow(id);
        if (sale.getStatus() == FlashSaleStatus.PUBLISHED) {
            service.setStatus(id, FlashSaleStatus.PAUSED);
            redirect.addFlashAttribute("toast", "Đã tạm dừng chương trình Flash Sale");
        } else if (sale.getStatus() == FlashSaleStatus.PAUSED) {
            service.setStatus(id, FlashSaleStatus.PUBLISHED);
            redirect.addFlashAttribute("toast", "Đã mở lại chương trình Flash Sale");
        } else {
            redirect.addFlashAttribute("toast", "Chương trình đang ở trạng thái Nháp, hãy đăng chương trình trước.");
            redirect.addFlashAttribute("toastType", "error");
            return "redirect:/admin/flash-sales";
        }
        redirect.addFlashAttribute("toastType", "success");
        return "redirect:/admin/flash-sales";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Integer id, RedirectAttributes redirect) {
        try {
            service.delete(id);
            redirect.addFlashAttribute("toast", "Đã xóa chương trình Flash Sale");
            redirect.addFlashAttribute("toastType", "success");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("toast", e.getMessage());
            redirect.addFlashAttribute("toastType", "error");
        }
        return "redirect:/admin/flash-sales";
    }
}
