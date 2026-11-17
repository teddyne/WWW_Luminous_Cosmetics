package com.luminous.controller.admin;

import com.luminous.dto.PasswordForm;
import com.luminous.dto.ProfileForm;
import com.luminous.model.User;
import com.luminous.repository.UserRepository;
import com.luminous.security.CustomUserDetails;
import com.luminous.service.FileStorageService;
import com.luminous.service.admin.ShopSettingsService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/settings")
public class AdminSettingsController {

    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final ShopSettingsService settings;
    private final FileStorageService files;

    public AdminSettingsController(UserRepository users, PasswordEncoder passwords,
                                   ShopSettingsService settings, FileStorageService files) {
        this.users = users;
        this.passwords = passwords;
        this.settings = settings;
        this.files = files;
    }

    @GetMapping
    public String settings(@AuthenticationPrincipal CustomUserDetails admin, Model model) {
        if (!model.containsAttribute("profileForm")) {
            ProfileForm form = new ProfileForm();
            form.setFullName(admin.getUser().getFullName());
            form.setPhone(admin.getUser().getPhone());
            model.addAttribute("profileForm", form);
        }
        if (!model.containsAttribute("passwordForm")) {
            model.addAttribute("passwordForm", new PasswordForm());
        }
        model.addAttribute("settings", settings.all());
        model.addAttribute("activeMenu", "settings");
        return "admin/settings";
    }

    @PostMapping("/store")
    public String updateStore(@RequestParam(defaultValue = "") String shopName,
                              @RequestParam(defaultValue = "") String shopAddress,
                              @RequestParam(defaultValue = "") String shopHotline,
                              @RequestParam(defaultValue = "") String shopEmail,
                              @RequestParam(required = false) MultipartFile logo,
                              RedirectAttributes redirect) {
        Map<String, String> values = new HashMap<>();
        values.put("shop.name", shopName);
        values.put("shop.address", shopAddress);
        values.put("shop.hotline", shopHotline);
        values.put("shop.email", shopEmail);
        List<String> warnings = new ArrayList<>();
        String logoUrl = storeImage(logo, "logo", warnings);
        if (logoUrl != null) {
            values.put("shop.logo", logoUrl);
        }
        List<String> failed = settings.saveAll(values);
        warnings.addAll(failed);
        finishSave(redirect, "Đã lưu thông tin cửa hàng", warnings);
        return "redirect:/admin/settings";
    }

    @PostMapping("/shipping")
    public String updateShipping(@RequestParam(defaultValue = "30000") String shippingFee,
                                 @RequestParam(defaultValue = "") String shippingProviders,
                                 RedirectAttributes redirect) {
        save(redirect, "Đã lưu cài đặt vận chuyển",
                Map.of("shipping.fee", shippingFee, "shipping.providers", shippingProviders));
        return "redirect:/admin/settings";
    }

    @PostMapping("/payment")
    public String updatePayment(@RequestParam(defaultValue = "false") String paymentCod,
                                @RequestParam(defaultValue = "false") String paymentBanking,
                                @RequestParam(defaultValue = "") String bankName,
                                @RequestParam(defaultValue = "") String bankAccount,
                                @RequestParam(defaultValue = "") String bankHolder,
                                RedirectAttributes redirect) {
        save(redirect, "Đã lưu phương thức thanh toán",
                Map.of("payment.cod", paymentCod, "payment.banking", paymentBanking,
                        "payment.bankName", bankName, "payment.bankAccount", bankAccount,
                        "payment.bankHolder", bankHolder));
        return "redirect:/admin/settings";
    }

    @PostMapping("/ai")
    public String updateAi(@RequestParam(defaultValue = "false") String aiEnabled,
                           @RequestParam(defaultValue = "20") String aiDailyLimit,
                           @RequestParam(defaultValue = "") String aiGreeting,
                           RedirectAttributes redirect) {
        save(redirect, "Đã lưu cài đặt Luminous AI",
                Map.of("ai.enabled", aiEnabled, "ai.dailyLimit", aiDailyLimit,
                        "ai.greeting", aiGreeting));
        return "redirect:/admin/settings";
    }

    private void save(RedirectAttributes redirect, String okMessage, Map<String, String> values) {
        finishSave(redirect, okMessage, settings.saveAll(values));
    }

    private void finishSave(RedirectAttributes redirect, String okMessage, List<String> warnings) {
        if (warnings.isEmpty()) {
            redirect.addFlashAttribute("toast", okMessage);
        } else {
            redirect.addFlashAttribute("toast", okMessage + ". Chưa lưu: " + String.join("; ", warnings));
        }
        redirect.addFlashAttribute("toastType", "success");
    }

    private String storeImage(MultipartFile file, String baseName, List<String> warnings) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        try {
            return files.store(file, "images", baseName);
        } catch (Exception e) {
            warnings.add("Ảnh " + baseName + " không lưu được (" + e.getMessage() + ")");
            return null;
        }
    }

    @PostMapping("/profile")
    public String updateProfile(@Valid @ModelAttribute("profileForm") ProfileForm form,
                                BindingResult result,
                                @AuthenticationPrincipal CustomUserDetails admin,
                                Model model,
                                RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return settings(admin, model);
        }
        User user = users.findById(admin.getUser().getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản"));
        String phone = form.getPhone() == null || form.getPhone().isBlank() ? null : form.getPhone().trim();
        if (phone != null && users.findByPhone(phone)
                .map(other -> !other.getId().equals(user.getId())).orElse(false)) {
            model.addAttribute("formError", "Số điện thoại đã được dùng bởi tài khoản khác");
            return settings(admin, model);
        }
        user.setFullName(form.getFullName().trim());
        user.setPhone(phone);
        users.save(user);
        redirect.addFlashAttribute("toast", "Đã cập nhật thông tin tài khoản");
        redirect.addFlashAttribute("toastType", "success");
        return "redirect:/admin/settings";
    }

    @PostMapping("/password")
    public String changePassword(@Valid @ModelAttribute("passwordForm") PasswordForm form,
                                 BindingResult result,
                                 @AuthenticationPrincipal CustomUserDetails admin,
                                 Model model,
                                 RedirectAttributes redirect) {
        User user = users.findById(admin.getUser().getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản"));
        if (!result.hasErrors() && !passwords.matches(form.getCurrentPassword(), user.getPassword())) {
            result.rejectValue("currentPassword", "mismatch", "Mật khẩu hiện tại không đúng");
        }
        if (!result.hasErrors() && !form.getNewPassword().equals(form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "mismatch", "Mật khẩu nhập lại không khớp");
        }
        if (result.hasErrors()) {
            return settings(admin, model);
        }
        user.setPassword(passwords.encode(form.getNewPassword()));
        users.save(user);
        redirect.addFlashAttribute("toast", "Đã đổi mật khẩu");
        redirect.addFlashAttribute("toastType", "success");
        return "redirect:/admin/settings";
    }
}
