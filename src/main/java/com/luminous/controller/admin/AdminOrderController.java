package com.luminous.controller.admin;

import com.luminous.dto.OrderCreateForm;
import com.luminous.security.CustomUserDetails;
import com.luminous.service.admin.AdminOrderService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.io.IOException;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin/orders")
public class AdminOrderController {

    private final AdminOrderService service;

    public AdminOrderController(AdminOrderService service) {
        this.service = service;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String status,
                       @RequestParam(required = false) String q,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       @RequestParam(required = false) String channel,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       Model model) {
        model.addAttribute("orders", service.page(status, q, from, to, channel, page, size));
        model.addAttribute("status", status);
        model.addAttribute("q", q);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("channel", channel);
        model.addAttribute("baseUrl", "/admin/orders");
        model.addAttribute("pageQuery", AdminPageLinks.query(
                "status", status, "q", q, "from", from, "to", to, "channel", channel));
        model.addAttribute("activeMenu", "orders");
        return "admin/orders";
    }

    @GetMapping("/new")
    public String createForm(@RequestParam(required = false) Integer variantId, Model model) {
        if (!model.containsAttribute("orderForm")) {
            OrderCreateForm form = new OrderCreateForm();
            form.setShippingFee(BigDecimal.valueOf(30000));
            form.setDiscountAmount(BigDecimal.ZERO);
            model.addAttribute("orderForm", form);
        }
        model.addAttribute("customers", service.customers());
        model.addAttribute("variants", service.activeVariants());
        model.addAttribute("preselectedVariantId", variantId);
        model.addAttribute("activeMenu", "orders");
        return "admin/order-form";
    }

    @PostMapping
    public String create(@ModelAttribute("orderForm") OrderCreateForm form,
                         @RequestParam(defaultValue = "save") String action,
                         @AuthenticationPrincipal CustomUserDetails admin,
                         RedirectAttributes redirect) {
        try {
            Integer adminId = admin == null ? null : admin.getUser().getId();
            Integer id = service.createOrder(form, adminId);
            redirect.addFlashAttribute("toast", "Đã tạo đơn hàng mới");
            redirect.addFlashAttribute("toastType", "success");
            if ("save_print".equals(action)) {
                return "redirect:/admin/orders/" + id + "?print=1";
            }
            return "redirect:/admin/orders/" + id;
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("toast", e.getMessage());
            redirect.addFlashAttribute("toastType", "error");
            redirect.addFlashAttribute("orderForm", form);
            return "redirect:/admin/orders/new";
        }
    }

    @PostMapping("/bulk-status")
    public String bulkStatus(@RequestParam(required = false) List<Integer> ids,
                             @RequestParam(required = false) String bulkAction,
                             @RequestParam(required = false) String newStatus,
                             RedirectAttributes redirect) {
        if (ids == null || ids.isEmpty()) {
            redirect.addFlashAttribute("toast", "Chưa chọn đơn hàng nào");
            redirect.addFlashAttribute("toastType", "error");
            return "redirect:/admin/orders";
        }
        // Nut "Xoa da chon" o thanh bulk: chi an don khoi danh sach,
        // khong xoa khoi database.
        if ("delete".equals(bulkAction)) {
            int done = service.bulkHide(ids);
            redirect.addFlashAttribute("toast", "Đã xóa " + done + " đơn hàng khỏi danh sách");
            redirect.addFlashAttribute("toastType", "success");
            return "redirect:/admin/orders";
        }
        if (newStatus == null || newStatus.isBlank()) {
            redirect.addFlashAttribute("toast", "Chưa chọn trạng thái mới");
            redirect.addFlashAttribute("toastType", "error");
            return "redirect:/admin/orders";
        }
        int[] done = service.bulkChangeStatus(ids, newStatus);
        redirect.addFlashAttribute("toast", "Đã cập nhật " + done[0] + " đơn hàng"
                + (done[1] > 0 ? ", bỏ qua " + done[1] + " đơn không đổi được sang trạng thái này" : ""));
        redirect.addFlashAttribute("toastType", "success");
        return "redirect:/admin/orders";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id,
                         @RequestParam(required = false) String print,
                         Model model) {
        var order = service.detail(id);
        model.addAttribute("order", order);
        model.addAttribute("stepItems", AdminOrderService.stepItems(order));
        model.addAttribute("lines", service.lines(id));
        model.addAttribute("nextStatusOptions", AdminOrderService.nextStatusOptions(order.getStatus()));
        model.addAttribute("statusLabel", AdminOrderService.statusLabel(order.getStatus()));
        model.addAttribute("payment", service.paymentOf(id));
        model.addAttribute("paymentMethod", service.paymentMethodLabel(id));
        model.addAttribute("createdByName", service.createdByName(order.getCreatedBy()));
        model.addAttribute("printDate", LocalDateTime.now());
        model.addAttribute("print", print);
        model.addAttribute("activeMenu", "orders");
        return "admin/order-detail";
    }

    @PostMapping("/{id}/save-bill")
    public String saveBill(@PathVariable Integer id, RedirectAttributes redirect) {
        try {
            String path = service.saveBill(id);
            redirect.addFlashAttribute("toast", "Đã lưu hóa đơn vào " + path);
            redirect.addFlashAttribute("toastType", "success");
        } catch (IOException e) {
            redirect.addFlashAttribute("toast", "Không lưu được hóa đơn: " + e.getMessage());
            redirect.addFlashAttribute("toastType", "error");
        }
        return "redirect:/admin/orders/" + id;
    }

    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Integer id,
                               @RequestParam String newStatus,
                               @RequestParam(required = false) String cancelReason,
                               RedirectAttributes redirect) {
        try {
            service.changeStatus(id, newStatus, cancelReason);
            redirect.addFlashAttribute("toast", "Đã cập nhật trạng thái đơn hàng");
            redirect.addFlashAttribute("toastType", "success");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("toast", e.getMessage());
            redirect.addFlashAttribute("toastType", "error");
        }
        return "redirect:/admin/orders/" + id;
    }
}
